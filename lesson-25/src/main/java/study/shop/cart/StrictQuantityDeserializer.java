package study.shop.cart;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

// JSON 문자열이나 소수를 정수 수량으로 자동 변환하지 않습니다.
public class StrictQuantityDeserializer extends ValueDeserializer<Integer> {
    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return context.reportInputMismatch(Integer.class,
                    "수량에는 JSON 정수를 입력해야 합니다.");
        }
        return parser.getIntValue();
    }
}
