import http from "node:http";

const port = process.env.PORT || 3000;

const server = http.createServer((req, res) => {
  res.setHeader("Content-Type", "application/json");
  if (req.method === "GET" && req.url === "/health") {
    res.writeHead(200);
    res.end(JSON.stringify({ ok: true }));
    return;
  }
  res.writeHead(404);
  res.end(JSON.stringify({ error: "Not found" }));
});

server.listen(port, () => console.log(`Field Assistant backend listening on ${port}`));
