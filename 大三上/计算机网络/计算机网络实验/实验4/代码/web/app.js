const hostLabel = document.getElementById("hostLabel");
const statusEl = document.getElementById("status");
const nameInput = document.getElementById("name");
const connectBtn = document.getElementById("connectBtn");
const logEl = document.getElementById("log");
const userListEl = document.getElementById("userList");
const textInput = document.getElementById("text");
const sendBtn = document.getElementById("sendBtn");
const fileBtn = document.getElementById("fileBtn");
const imgBtn = document.getElementById("imgBtn");
const fileInput = document.getElementById("fileInput");

hostLabel.innerText = location.host;

let ws = null;
let selected = new Set();
let currentUser = "";
const avatarCache = new Map();

function avatarFor(name) {
  if (!name) return null;
  if (avatarCache.has(name)) return avatarCache.get(name);
  const emojis = ["😺", "😸", "😻", "🐱", "🐾", "🐼", "🐰", "🦊", "🐯", "🐹", "🦁", "🐮", "🐨"];
  const colors = ["#f8c8dc", "#c6e5ff", "#ffe9c0", "#d9f4c7", "#e5d1ff", "#ffd6a5", "#c2f0f7", "#fce1e4", "#e0f1ff", "#fff4c2"];
  let h = 0;
  for (let i = 0; i < name.length; i++) h = (h * 131 + name.charCodeAt(i)) >>> 0;
  const emo = emojis[h % emojis.length];
  const color = colors[h % colors.length];
  const canvas = document.createElement("canvas");
  canvas.width = canvas.height = 128;
  const ctx = canvas.getContext("2d");
  ctx.fillStyle = color;
  ctx.fillRect(0, 0, 128, 128);
  ctx.fillStyle = "#333";
  ctx.font = "96px 'Apple Color Emoji','Segoe UI Emoji','Noto Color Emoji','Arial'";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(emo, 64, 75);
  const url = canvas.toDataURL("image/png");
  avatarCache.set(name, url);
  return url;
}

function logText({ who = "", text = "", self = false }) {
  const row = document.createElement("div");
  row.className = `row ${self ? "me" : "other"}`;

  const avatarUrl = avatarFor(who);
  if (avatarUrl) {
    const avatarWrap = document.createElement("div");
    avatarWrap.className = "avatar";
    const img = document.createElement("img");
    img.src = avatarUrl;
    avatarWrap.appendChild(img);
    row.appendChild(avatarWrap);
  }

  const div = document.createElement("div");
  div.className = `msg ${self ? "me" : "other"}`;
  if (who) {
    const name = document.createElement("span");
    name.className = "name";
    name.innerText = self ? "我" : who;
    div.appendChild(name);
  }
  const body = document.createElement("span");
  body.innerText = text;
  div.appendChild(body);

  row.appendChild(div);
  logEl.appendChild(row);
  logEl.scrollTop = logEl.scrollHeight;
}

function logSys(text) {
  const div = document.createElement("div");
  div.className = "sys";
  div.innerText = text;
  logEl.appendChild(div);
  logEl.scrollTop = logEl.scrollHeight;
}

function setStatus(s) {
  statusEl.innerText = s;
}

function encodeMessage(obj) {
  const params = new URLSearchParams();
  Object.entries(obj).forEach(([k, v]) => {
    if (v === undefined || v === null || v === "") return;
    if (Array.isArray(v)) {
      if (v.length === 0) return;
      params.set(k, v.join(","));
    } else {
      params.set(k, v);
    }
  });
  return params.toString();
}

function parseMessage(str) {
  const p = new URLSearchParams(str);
  const get = (k) => p.has(k) ? p.get(k) : undefined;
  const split = (k) => p.has(k) && p.get(k) !== "" ? p.get(k).split(",") : [];
  return {
    type: get("type"),
    from: get("from"),
    text: get("text"),
    fileName: get("fileName"),
    data: get("data"),
    note: get("note"),
    onlineUsers: split("onlineUsers"),
    targets: split("targets"),
  };
}

function connect() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.close();
  }
  const name = nameInput.value.trim();
  if (!name) {
    alert("请输入用户名");
    return;
  }
  currentUser = name;
  ws = new WebSocket(`ws://${location.host}/ws`);
  ws.onopen = () => {
    setStatus("已连接");
    ws.send(encodeMessage({ type: "LOGIN", from: name, timestamp: Date.now() }));
  };
  ws.onmessage = (ev) => handleIncoming(ev.data);
  ws.onclose = () => setStatus("未连接");
  ws.onerror = () => setStatus("错误");
}

function handleIncoming(data) {
  const msg = parseMessage(data);
  switch (msg.type) {
    case "LOGIN_OK":
    case "USER_LIST":
      renderUsers(msg.onlineUsers || []);
      log("[系统] 在线人数: " + (msg.onlineUsers || []).length, "sys");
      break;
    case "SYSTEM":
      logSys("[系统] " + (msg.text || ""));
      break;
    case "CHAT":
      // 自己发出的消息服务器会回显，避免重复显示
      if (msg.from !== currentUser) {
        logText({ who: msg.from, text: msg.text || "", self: false });
      }
      break;
    case "FILE":
    case "IMAGE":
      if (msg.from !== currentUser) {
        renderFile(msg);
      }
      break;
    default:
      break;
  }
}

function renderUsers(users) {
  userListEl.innerHTML = "";
  users.forEach(u => {
    const li = document.createElement("li");
    li.innerText = u;
    if (selected.has(u)) li.classList.add("active");
    li.onclick = () => {
      if (selected.has(u)) selected.delete(u);
      else selected.add(u);
      renderUsers(users);
    };
    userListEl.appendChild(li);
  });
}

function currentTargets() {
  return Array.from(selected);
}

function sendText() {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    alert("未连接");
    return;
  }
  const text = textInput.value.trim();
  if (!text) return;
  ws.send(encodeMessage({ type: "CHAT", text, targets: currentTargets(), timestamp: Date.now() }));
  logText({ who: currentUser, text, self: true });
  textInput.value = "";
}

async function sendFile(isImage) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    alert("未连接");
    return;
  }
  fileInput.accept = isImage ? "image/*" : "*/*";
  fileInput.onchange = async () => {
    const file = fileInput.files[0];
    if (!file) return;
    const base64 = await fileToBase64(file);
    ws.send(encodeMessage({
      type: isImage ? "IMAGE" : "FILE",
      fileName: file.name,
      data: base64,
      targets: currentTargets(),
      timestamp: Date.now()
    }));
    // 本地立即渲染自己的文件/图片气泡
    renderFile({
      from: currentUser,
      fileName: file.name,
      data: base64,
      type: isImage ? "IMAGE" : "FILE"
    });
    fileInput.value = "";
  };
  fileInput.click();
}

function renderFile(msg) {
  const who = msg.from || "匿名";
  const name = msg.fileName || "文件";
  const base64 = msg.data || "";
  const isImg = msg.type === "IMAGE";
  const mime = isImg ? "image/*" : "application/octet-stream";

  const link = document.createElement("a");
  link.innerText = `${who} 发送 ${isImg ? "图片" : "文件"}: ${name}`;
  link.download = name;
  link.href = `data:${mime};base64,${base64}`;

  const row = document.createElement("div");
  const isMe = msg.from === currentUser;
  row.className = `row ${isMe ? "me" : "other"}`;

  const avatarUrl = avatarFor(who);
  if (avatarUrl) {
    const avatarWrap = document.createElement("div");
    avatarWrap.className = "avatar";
    const img = document.createElement("img");
    img.src = avatarUrl;
    avatarWrap.appendChild(img);
    row.appendChild(avatarWrap);
  }

  const div = document.createElement("div");
  div.className = `msg ${isMe ? "me" : "other"}`;
  const nameSpan = document.createElement("span");
  nameSpan.className = "name";
  nameSpan.innerText = msg.from === currentUser ? "我" : who;
  div.appendChild(nameSpan);
  div.appendChild(link);

  if (isImg && base64) {
    const img = document.createElement("img");
    img.src = `data:${mime};base64,${base64}`;
    img.alt = name;
    img.style.maxWidth = "320px";
    img.style.display = "block";
    img.style.marginTop = "6px";
    div.appendChild(img);
  }

  row.appendChild(div);
  logEl.appendChild(row);
  logEl.scrollTop = logEl.scrollHeight;
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result.split(",")[1];
      resolve(result);
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

connectBtn.onclick = connect;
sendBtn.onclick = sendText;
textInput.onkeydown = (e) => {
  if (e.key === "Enter") sendText();
};
fileBtn.onclick = () => sendFile(false);
imgBtn.onclick = () => sendFile(true);

// 页面加载后生成随机用户名并自动连接
window.addEventListener("load", () => {
  const rand = Math.floor(100 + Math.random() * 900);
  const autoName = `用户_${rand}`;
  nameInput.value = autoName;
  currentUser = autoName;
  connect();
});
