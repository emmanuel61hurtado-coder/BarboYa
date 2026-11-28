package com.barboya.barboya.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "barboya")
@Getter
@Setter
public class BarboyaProperties {
    private String appName = "Barboya API";
    private String environment = "development";
    private Jwt jwt = new Jwt();
    private List<String> corsOrigins = List.of("*");
    private int priceDailyCop = 3000;
    private int priceWeeklyCop = 15000;
    private int priceMonthlyCop = 50000;
    private int defaultTripFeeCop = 300;
    private int stoppedAlertSeconds = 180;
    private int faceCheckIntervalMinutes = 240;
    private int offerTtlSeconds = 120;
    private String s3Bucket = "barboya-media";
    private String chainRpcUrl = "";
    private long chainId = 80002;
    private String paymentTokenAddress = "";
    private String paymentTokenSymbol = "USDC";
    private int paymentTokenDecimals = 6;
    private String treasuryAddress = "";
    private int copPerToken = 4000;
    private int requiredConfirmations = 12;
    private int topupMinCop = 1000;
    private int topupMaxCop = 500000;
    private double indexerIntervalSeconds = 15.0;

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "CAMBIAR-EN-PRODUCCION";
        private String algorithm = "HS256";
        private int expireMinutes = 1440;
        private String issuer = "barboya-auth";
        private String audience = "barboya-api";
    }
}