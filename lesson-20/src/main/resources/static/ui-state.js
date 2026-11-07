let busy = false;

export function isBusy() {
  return busy;
}

export function setBusy(value) {
  busy = value;
  const main = document.querySelector("main");
  main.setAttribute("aria-busy", String(value));
  for (const control of main.querySelectorAll("button, input, select")) {
    control.disabled = value;
  }
}
