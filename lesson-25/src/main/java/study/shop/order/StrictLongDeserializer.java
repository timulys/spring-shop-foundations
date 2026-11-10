package study.shop.order;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

// 확인 금액과 장바구니 버전은 JSON 정수로 받습니다.
public class StrictLongDeserializer extends ValueDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return context.reportInputMismatch(Long.class, "JSON 정수를 입력해 주세요.");
        }
        return parser.getLongValue();
    }
}
