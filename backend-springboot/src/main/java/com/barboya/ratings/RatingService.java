package com.barboya.ratings;

import com.barboya.audit.AuditAction;
import com.barboya.audit.AuditService;
import com.barboya.businesses.Business;
import com.barboya.businesses.BusinessRepository;
import com.barboya.common.ApiException;
import com.barboya.common.ClientInfo;
import com.barboya.orders.OrderAccess;
import com.barboya.ratings.RatingDtos.Mine;
import com.barboya.ratings.RatingDtos.Moderated;
import com.barboya.ratings.RatingDtos.ModeratedPage;
import com.barboya.ratings.RatingDtos.NewRating;
import com.barboya.ratings.RatingDtos.OrderRatings;
import com.barboya.ratings.RatingDtos.Review;
import com.barboya.ratings.RatingDtos.Reviews;
import com.barboya.ratings.RatingDtos.Summary;
import com.barboya.ratings.RatingDtos.TargetRating;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class RatingService {

    static final Duration WINDOW = Duration.ofDays(7);
    private static final Summary NONE = new Summary(null, 0);

    private final RatingRepository ratings;
    private final OrderAccess orders;
    private final BusinessRepository businesses;
    private final UserRepository users;
    private final AuditService audit;
    private final Clock clock;

    public RatingService(RatingRepository ratings, OrderAccess orders, BusinessRepository businesses,
                         UserRepository users, AuditService audit, Clock clock) {
        this.ratings = ratings;
        this.orders = orders;
        this.businesses = businesses;
        this.users = users;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OrderRatings forOrder(UUID customerId, UUID orderId) {
        return view(requireOwnOrder(customerId, orderId), orderId);
    }

    public OrderRatings rate(UUID customerId, UUID orderId, NewRating request) {
        OrderAccess.Parties order = requireOwnOrder(customerId, orderId);
        if (!order.delivered()) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_DELIVERED",
                    "Solo puedes calificar un pedido entregado");
        }
        if (openUntil(order) == null) {
            throw new ApiException(HttpStatus.CONFLICT, "RATING_CLOSED", "El plazo para calificar ya termino");
        }
        UUID target = targetId(order, request.target());
        if (target == null) {
            throw new ApiException(HttpStatus.CONFLICT, "NOTHING_TO_RATE",
                    "Este pedido no tuvo domiciliario de BarboYa");
        }
        if (ratings.findByOrderId(orderId).stream().anyMatch(r -> r.getTargetType() == request.target())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_RATED", "Ya calificaste este pedido");
        }
        String comment = request.comment() == null || request.comment().isBlank() ? null : request.comment().trim();
        ratings.saveAndFlush(new Rating(orderId, customerId, request.target(), target, request.stars(), comment,
                clock.instant()));
        return view(order, orderId);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Summary> summaries(Rating.Target type, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Summary> found = ratings.totals(type, ids).stream()
                .collect(Collectors.toMap(RatingRepository.Totals::getTargetId,
                        t -> new Summary(Math.round(t.getAverage() * 10) / 10.0, t.getCount())));
        return ids.stream().distinct()
                .collect(Collectors.toMap(Function.identity(), id -> found.getOrDefault(id, NONE)));
    }

    /** Opiniones publicas de un comercio: solo las que traen comentario. */
    @Transactional(readOnly = true)
    public Reviews publicReviews(UUID businessId, int page) {
        return reviews(Rating.Target.BUSINESS, businessId,
                ratings.findByTargetTypeAndTargetIdAndHiddenFalseAndCommentNotNull(
                        Rating.Target.BUSINESS, businessId, newest(page)));
    }

    /** Lo que recibe un comercio o domiciliario, con o sin comentario. */
    @Transactional(readOnly = true)
    public Reviews received(Rating.Target type, UUID userId, int page) {
        UUID target = type == Rating.Target.COURIER
                ? userId : businesses.findByOwnerId(userId).map(Business::getId).orElse(null);
        if (target == null) {
            return new Reviews(NONE, List.of(), 0, 20, 0);
        }
        return reviews(type, target, ratings.findByTargetTypeAndTargetIdAndHiddenFalse(type, target, newest(page)));
    }

    @Transactional(readOnly = true)
    public ModeratedPage moderationList(boolean hidden, int page) {
        Page<Rating> result = ratings.findByHidden(hidden, newest(page));
        return new ModeratedPage(moderated(result.getContent()),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public Moderated hide(UUID staffId, UUID ratingId, String reason, ClientInfo client) {
        return moderate(staffId, ratingId, true, reason.trim(), client);
    }

    public Moderated restore(UUID staffId, UUID ratingId, String reason, ClientInfo client) {
        return moderate(staffId, ratingId, false, reason.trim(), client);
    }

    private Moderated moderate(UUID staffId, UUID ratingId, boolean hide, String reason, ClientInfo client) {
        Rating rating = ratings.findById(ratingId).orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "RATING_NOT_FOUND", "Calificacion no encontrada"));
        if (rating.isHidden() == hide) {
            throw new ApiException(HttpStatus.CONFLICT, "NO_CHANGE", "La calificacion ya esta en ese estado");
        }
        if (hide) {
            rating.hide(reason);
        } else {
            rating.restore();
        }
        audit.record(hide ? AuditAction.RATING_HIDDEN : AuditAction.RATING_RESTORED, staffId, rating.getAuthorId(),
                "calificacion " + rating.getId() + ": " + reason, client);
        ratings.flush();
        return moderated(List.of(rating)).getFirst();
    }

    private List<Moderated> moderated(List<Rating> list) {
        Map<UUID, String> people = users.findAllById(list.stream()
                        .flatMap(r -> Arrays.stream(new UUID[]{r.getAuthorId(),
                                r.getTargetType() == Rating.Target.COURIER ? r.getTargetId() : r.getAuthorId()}))
                        .distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, User::getFullName));
        Map<UUID, String> stores = businesses.findAllById(list.stream()
                        .filter(r -> r.getTargetType() == Rating.Target.BUSINESS)
                        .map(Rating::getTargetId).distinct().toList())
                .stream().collect(Collectors.toMap(Business::getId, Business::getName));
        return list.stream()
                .map(r -> new Moderated(r.getId(), r.getOrderId(), people.get(r.getAuthorId()), r.getTargetType(),
                        (r.getTargetType() == Rating.Target.BUSINESS ? stores : people).get(r.getTargetId()),
                        r.getStars(), r.getComment(), r.isHidden(), r.getHiddenReason(), r.getCreatedAt()))
                .toList();
    }

    private Reviews reviews(Rating.Target type, UUID target, Page<Rating> result) {
        return new Reviews(summaries(type, List.of(target)).get(target),
                result.map(Review::from).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private OrderRatings view(OrderAccess.Parties order, UUID orderId) {
        List<Rating> own = ratings.findByOrderId(orderId);
        List<TargetRating> targets = Arrays.stream(Rating.Target.values())
                .filter(type -> targetId(order, type) != null)
                .map(type -> {
                    UUID id = targetId(order, type);
                    Mine mine = own.stream().filter(r -> r.getTargetType() == type).findFirst()
                            .map(r -> new Mine(r.getStars(), r.getComment())).orElse(null);
                    return new TargetRating(type, summaries(type, List.of(id)).get(id), mine);
                })
                .toList();
        return new OrderRatings(openUntil(order), targets);
    }

    private Instant openUntil(OrderAccess.Parties order) {
        if (!order.delivered()) {
            return null;
        }
        Instant limit = order.deliveredAt().plus(WINDOW);
        return clock.instant().isBefore(limit) ? limit : null;
    }

    private static UUID targetId(OrderAccess.Parties order, Rating.Target type) {
        return type == Rating.Target.BUSINESS ? order.businessId() : order.courierId();
    }

    private OrderAccess.Parties requireOwnOrder(UUID customerId, UUID orderId) {
        return orders.parties(orderId)
                .filter(p -> p.customerId().equals(customerId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Pedido no encontrado"));
    }

    private static PageRequest newest(int page) {
        return PageRequest.of(Math.max(page, 0), 20, Sort.by("createdAt").descending());
    }
}
