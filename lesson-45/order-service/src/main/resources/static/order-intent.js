// A tab keeps one unresolved order per signed-in member, including across reloads.
export class OrderIntentStore {
  constructor(storage, memberId, newKey = () => crypto.randomUUID()) {
    if (!Number.isSafeInteger(memberId) || memberId <= 0) throw new Error("Invalid member");
    this.storage = storage;
    this.storageKey = "shop.pending-order.v1." + memberId;
    this.newKey = newKey;
    const saved = storage.getItem(this.storageKey);
    this.pending = saved === null ? null : JSON.parse(saved);
    if (this.pending !== null) this.validate(this.pending);
  }
  validate(intent) {
    if (!intent || typeof intent.key !== "string" || !/^[A-Za-z0-9_-]{16,80}$/.test(intent.key)
        || !Number.isSafeInteger(intent.body?.cartRevision) || intent.body.cartRevision < 0
        || !Number.isSafeInteger(intent.body?.expectedTotalWon) || intent.body.expectedTotalWon < 0) {
      throw new Error("Invalid saved order intent");
    }
  }
  begin(body) {
    if (this.pending !== null) {
      if (body.cartRevision !== this.pending.body.cartRevision
          || body.expectedTotalWon !== this.pending.body.expectedTotalWon) {
        throw new Error("Resolve the pending order first");
      }
      return this.pending;
    }
    const intent = { key: this.newKey(), body: {
      cartRevision: body.cartRevision, expectedTotalWon: body.expectedTotalWon
    }};
    this.validate(intent);
    // Save before sending. A storage failure must not send an untracked order.
    this.storage.setItem(this.storageKey, JSON.stringify(intent));
    this.pending = intent;
    return intent;
  }
  clear() {
    this.storage.removeItem(this.storageKey);
    this.pending = null;
  }
}
