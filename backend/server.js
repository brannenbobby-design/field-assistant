import http from "node:http";
import OpenAI from "openai";

const port = process.env.PORT || 3000;
const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

function send(res, status, body) {
  res.writeHead(status, { "Content-Type": "application/json" });
  res.end(JSON.stringify(body));
}

async function readJson(req) {
  let raw = "";
  for await (const chunk of req) {
    raw += chunk;
    if (raw.length > 100000) throw new Error("Request too large");
  }
  return JSON.parse(raw || "{}");
}

const server = http.createServer(async (req, res) => {
  if (req.method === "GET" && req.url === "/health") {
    send(res, 200, { ok: true });
    return;
  }

  if (req.method === "POST" && req.url === "/ask") {
    try {
      if (!process.env.OPENAI_API_KEY) {
        send(res, 503, { error: "OPENAI_API_KEY is not configured" });
        return;
      }

      const { message } = await readJson(req);
      if (typeof message !== "string" || !message.trim()) {
        send(res, 400, { error: "message is required" });
        return;
      }

      const response = await client.responses.create({
        model: process.env.OPENAI_MODEL || "gpt-5",
        instructions: "You are Field Assistant, a concise hands-free field helper. Give direct, practical answers that are easy to understand when spoken aloud. Ask a short clarifying question when critical information is missing. Never pretend to see, hear, measure, inspect, or verify something you have not been given.",
        input: message.trim(),
      });

      const reply = response.output_text?.trim();
      if (!reply) throw new Error("OpenAI returned an empty reply");
      send(res, 200, { reply });
    } catch (error) {
      console.error(error);
      send(res, 500, { error: "Assistant request failed" });
    }
    return;
  }

  send(res, 404, { error: "Not found" });
});

server.listen(port, () => console.log(`Field Assistant backend listening on ${port}`));
