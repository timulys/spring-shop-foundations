export function productsPath(category, keyword) {
  const params = new URLSearchParams();
  if (category) params.set("category", category);
  const query = keyword.trim();
  if (query) params.set("q", query);
  const search = params.toString();
  return "/api/products" + (search ? "?" + search : "");
}
