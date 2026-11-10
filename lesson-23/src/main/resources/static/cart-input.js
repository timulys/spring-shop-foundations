export function cartQuantity(value) {
  const quantity = Number(value);
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
    throw new RangeError("수량은 1부터 99까지의 정수로 입력해 주세요.");
  }
  return quantity;
}
