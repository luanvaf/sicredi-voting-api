const USER_PATH = /^\/users\/([^/]+)$/;
const INELIGIBLE_CPFS = new Set(["62289608068"]);

export default {
  async fetch(request) {
    const url = new URL(request.url);

    if (request.method !== "GET") {
      return new Response(null, {
        status: 405,
        headers: { Allow: "GET" },
      });
    }

    if (url.pathname === "/health") {
      return Response.json({ status: "UP" });
    }

    const match = USER_PATH.exec(url.pathname);
    if (!match) {
      return notFound();
    }

    let cpf;
    try {
      cpf = decodeURIComponent(match[1]);
    } catch {
      return notFound();
    }

    if (!isValidCpf(cpf)) {
      return notFound();
    }

    const status = INELIGIBLE_CPFS.has(cpf)
      ? "UNABLE_TO_VOTE"
      : "ABLE_TO_VOTE";
    return Response.json({ status });
  },
};

function notFound() {
  return Response.json({ error: "Associate not found" }, { status: 404 });
}

function isValidCpf(cpf) {
  if (!/^\d{11}$/.test(cpf) || /^(\d)\1{10}$/.test(cpf)) {
    return false;
  }

  const firstDigit = calculateCheckDigit(cpf, 9, 10);
  const secondDigit = calculateCheckDigit(cpf, 10, 11);
  return firstDigit === Number(cpf[9]) && secondDigit === Number(cpf[10]);
}

function calculateCheckDigit(cpf, digitCount, initialWeight) {
  let sum = 0;
  for (let index = 0; index < digitCount; index += 1) {
    sum += Number(cpf[index]) * (initialWeight - index);
  }

  const digit = 11 - (sum % 11);
  return digit >= 10 ? 0 : digit;
}
