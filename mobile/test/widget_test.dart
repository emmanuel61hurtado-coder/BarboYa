import 'package:flutter_test/flutter_test.dart';

import 'package:barboya/main.dart';

void main() {
  testWidgets('BarboyaApp smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const BarboyaApp());
    expect(find.byType(BarboyaApp), findsOneWidget);
  });
}
