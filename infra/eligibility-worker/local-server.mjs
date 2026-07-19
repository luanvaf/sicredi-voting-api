import http from "node:http";
import worker from "./src/index.js";

const port = Number(process.env.PORT ?? 8787);

const server = http.createServer(async (incomingRequest, outgoingResponse) => {
  try {
    const requestUrl = new URL(
      incomingRequest.url,
      `http://${incomingRequest.headers.host ?? `localhost:${port}`}`,
    );
    const workerResponse = await worker.fetch(
      new Request(requestUrl, {
        method: incomingRequest.method,
        headers: incomingRequest.headers,
      }),
    );

    outgoingResponse.writeHead(
      workerResponse.status,
      Object.fromEntries(workerResponse.headers.entries()),
    );
    outgoingResponse.end(Buffer.from(await workerResponse.arrayBuffer()));
  } catch {
    outgoingResponse.writeHead(500, { "content-type": "application/json" });
    outgoingResponse.end(JSON.stringify({ error: "Internal server error" }));
  }
});

server.listen(port, "0.0.0.0");
