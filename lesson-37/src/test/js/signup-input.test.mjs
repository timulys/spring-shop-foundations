import test from "node:test";
import assert from "node:assert/strict";
import { passwordInputError } from "../../main/resources/static/signup-input.js";

test("ASCII and UTF-8 boundary inputs", () => {
  assert.equal(passwordInputError("a".repeat(64)), "");
  assert.equal(passwordInputError("가".repeat(24)), "");
  assert.match(passwordInputError("가".repeat(25)), /72바이트/);
  assert.match(passwordInputError("a".repeat(65)), /8~64/);
});
test("spaces stay in the value while all-blank and malformed input fail", () => {
  assert.equal(passwordInputError(" pass-word "), "");
  assert.match(passwordInputError(" ".repeat(8)), /공백/);
  assert.match(passwordInputError("abcdefg" + String.fromCharCode(0xd800)), /유니코드/);
});
