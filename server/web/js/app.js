// ==================== Toast ====================
const TOAST_ICONS = {
    success: '✓',
    error: '✕',
    info: 'ℹ',
    warning: '⚠',
};

// 显示 Toast 通知
function toast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const el = document.createElement('div');
    el.className = 'toast toast-' + type;
    el.innerHTML = `
        <span class="toast-icon">${TOAST_ICONS[type] || ''}</span>
        <span>${escHtml(message)}</span>
        <button class="toast-close" onclick="this.parentElement.remove()">✕</button>
    `;
    container.appendChild(el);
    setTimeout(() => el.remove(), 3000);
}

// HTML 转义
function escHtml(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
}

// 属性转义
function escAttr(s) {
    return s.replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// ==================== Modal ====================
function confirmModal({ title, message, confirmText = '确认', onConfirm }) {
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
        <div class="modal">
            <div class="modal-header">
                <div class="modal-icon danger">✕</div>
                <div class="modal-title">${escHtml(title)}</div>
            </div>
            <div class="modal-body">${escHtml(message)}</div>
            <div class="modal-actions">
                <button class="btn-cancel" id="modal-cancel">取消</button>
                <button class="btn-confirm-danger" id="modal-confirm">${escHtml(confirmText)}</button>
            </div>
        </div>
    `;

    const close = () => overlay.remove();

    overlay.querySelector('#modal-cancel').onclick = close;
    overlay.querySelector('#modal-confirm').onclick = () => { close(); onConfirm(); };
    overlay.addEventListener('click', (e) => { if (e.target === overlay) close(); });

    document.body.appendChild(overlay);
}

// ==================== API ====================
async function api(path, options) {
    const res = await fetch(path, options);
    return res.json();
}

// ==================== 远程控制 ====================
let ctrlEventSource = null;
let ctrlLogs = [];

// 初始化远程控制
function initControl() {
    loadControlStatus();
    setupControlSSE();
}

// 加载控制状态
async function loadControlStatus() {
    try {
        const status = await api('/api/control/status');
        updateControlUI(status);
    } catch (err) {
        console.error('加载控制状态失败:', err);
    }
}

// 更新控制界面
function updateControlUI(data) {
    // 更新 IP
    const ipEl = document.getElementById('ctrlIp');
    if (ipEl) ipEl.textContent = data.ip || '--';

    // 更新 WebSocket 地址
    const wsEl = document.getElementById('ctrlWsAddress');
    if (wsEl && data.wsAddress) wsEl.value = data.wsAddress;

    // 更新连接数
    const connEl = document.getElementById('ctrlConnections');
    if (connEl) connEl.textContent = data.connections || 0;

    // 更新状态指示器
    const indicator = document.getElementById('controlIndicator');
    if (indicator) {
        if (data.running) {
            indicator.className = 'indicator online';
            indicator.querySelector('.indicator-text').textContent = '运行中';
        } else {
            indicator.className = 'indicator offline';
            indicator.querySelector('.indicator-text').textContent = '离线';
        }
    }
}

// 设置 SSE 事件流
function setupControlSSE() {
    if (ctrlEventSource) {
        ctrlEventSource.close();
    }

    ctrlEventSource = new EventSource('/api/control/events');

    ctrlEventSource.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            if (data.type === 'status') {
                updateControlUI(data.data);
            } else if (data.type === 'log') {
                addCtrlLog(data.data);
            }
        } catch (err) {
            console.error('SSE 解析错误:', err);
        }
    };

    ctrlEventSource.onerror = () => {
        ctrlEventSource.close();
        // 重连
        setTimeout(setupControlSSE, 3000);
    };
}

// 添加控制日志
function addCtrlLog(msg) {
    ctrlLogs.unshift(msg);
    if (ctrlLogs.length > 100) ctrlLogs.pop();
    renderCtrlLogs();
}

// 渲染控制日志
function renderCtrlLogs() {
    const container = document.getElementById('ctrlLogs');
    if (!container) return;

    if (ctrlLogs.length === 0) {
        container.innerHTML = '<div class="logs-empty">暂无运行日志</div>';
        return;
    }

    container.innerHTML = ctrlLogs.map(log =>
        `<div class="log-entry">${escHtml(log)}</div>`
    ).join('');
}

// 清空控制日志
function clearCtrlLogs() {
    ctrlLogs = [];
    renderCtrlLogs();
}

// 运行诊断测试
async function runDiagnostic() {
    const btn = document.getElementById('btnDiagnostic');
    if (!btn) return;

    btn.disabled = true;
    addCtrlLog('开始诊断测试...');

    try {
        await api('/api/control/test', { method: 'POST' });
    } catch (err) {
        addCtrlLog('诊断测试失败: ' + err.message);
    } finally {
        btn.disabled = false;
    }
}

// 复制 WebSocket 地址
async function copyWsAddress() {
    const input = document.getElementById('ctrlWsAddress');
    if (!input) return;

    try {
        await navigator.clipboard.writeText(input.value);
        toast('地址已复制到剪贴板', 'success');
    } catch (err) {
        toast('复制失败', 'error');
    }
}

// 显示二维码
function showQrCode() {
    const ip = document.getElementById('ctrlIp');
    if (!ip || ip.textContent === '--') {
        toast('IP 地址不可用', 'warning');
        return;
    }

    const modal = document.getElementById('qrModal');
    const canvas = document.getElementById('qrCanvas');
    const urlEl = document.getElementById('qrUrl');

    if (!modal || !canvas) return;

    const wsAddress = document.getElementById('ctrlWsAddress').value;

    // 使用 qrcode.js 生成二维码
    if (typeof QRCode !== 'undefined') {
        QRCode.toCanvas(canvas, wsAddress, {
            width: 200,
            margin: 2,
            color: {
                dark: '#1e293b',
                light: '#ffffff'
            }
        }, function(error) {
            if (error) {
                console.error(error);
                toast('生成二维码失败', 'error');
            }
        });
    }

    if (urlEl) urlEl.textContent = wsAddress;
    modal.style.display = 'flex';
}

// 关闭二维码弹窗
function closeQrModal() {
    const modal = document.getElementById('qrModal');
    if (modal) modal.style.display = 'none';
}

// ==================== WebSocket ====================
let ws = null;

const statusDot = () => document.getElementById('statusDot');
const statusText = () => document.getElementById('statusText');
const btnConnect = () => document.getElementById('btnConnect');
const btnDisconnect = () => document.getElementById('btnDisconnect');
const msgInput = () => document.getElementById('msgInput');
const messages = () => document.getElementById('messages');

function setStatus(connected) {
    const dot = statusDot();
    const text = statusText();
    if (connected) {
        dot.className = 'dot connected';
        text.textContent = '已连接';
        btnConnect().disabled = true;
        btnDisconnect().disabled = false;
    } else {
        dot.className = 'dot disconnected';
        text.textContent = '未连接';
        btnConnect().disabled = false;
        btnDisconnect().disabled = true;
    }
}

function addMessage(text, type) {
    const div = document.createElement('div');
    div.className = 'msg ' + type;

    const content = document.createElement('div');
    content.textContent = text;

    const time = document.createElement('div');
    time.className = 'time';
    time.textContent = new Date().toLocaleTimeString();

    div.appendChild(content);
    div.appendChild(time);

    messages().appendChild(div);
    messages().scrollTop = messages().scrollHeight;
}

function wsConnect() {
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const url = proto + '//' + location.host + '/api/websocket';

    ws = new WebSocket(url);

    ws.onopen = () => {
        setStatus(true);
        addMessage('连接已建立', 'received');
        toast('WebSocket 已连接', 'success');
    };

    ws.onmessage = (e) => {
        try {
            const msg = JSON.parse(e.data);
            addMessage(msg.data, 'received');
        } catch {
            addMessage(e.data, 'received');
        }
    };

    ws.onclose = () => {
        setStatus(false);
        addMessage('连接已断开', 'received');
        toast('WebSocket 已断开', 'warning');
        ws = null;
    };

    ws.onerror = () => {
        addMessage('连接出错', 'received');
        toast('WebSocket 连接出错', 'error');
    };
}

function wsDisconnect() {
    if (ws) {
        ws.close();
        ws = null;
    }
}

function wsSend() {
    const input = msgInput();
    const text = input.value.trim();
    if (!text || !ws) return;

    ws.send(text);
    addMessage(text, 'sent');
    input.value = '';
    input.focus();
}

// ==================== CRUD ====================
const API = '/api/items';

async function loadItems() {
    const items = await api(API);
    const list = document.getElementById('itemList');

    if (!items || items.length === 0) {
        list.innerHTML = '<div class="empty-tip">暂无数据</div>';
        return;
    }

    list.innerHTML = items.map(item => `
        <div class="item-row" id="row-${item.id}">
            <span class="item-id">#${item.id}</span>
            <div class="item-name">
                <span id="name-${item.id}">${escHtml(item.name)}</span>
                <input id="edit-${item.id}" value="${escAttr(item.name)}" style="display:none">
            </div>
            <span class="item-time">${item.created_at || ''}</span>
            <button class="btn-sm btn-edit" id="ebtn-${item.id}" onclick="startEdit(${item.id})">编辑</button>
            <button class="btn-sm btn-save" id="sbtn-${item.id}" style="display:none" onclick="saveEdit(${item.id})">保存</button>
            <button class="btn-sm btn-del" onclick="deleteItem(${item.id})">删除</button>
        </div>
    `).join('');
}

async function createItem() {
    const input = document.getElementById('itemName');
    const name = input.value.trim();
    if (!name) {
        toast('请输入名称', 'warning');
        return;
    }

    const res = await api(API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name }),
    });

    if (res.error) {
        toast(res.error, 'error');
        return;
    }

    toast('添加成功', 'success');
    input.value = '';
    loadItems();
}

function startEdit(id) {
    document.getElementById('name-' + id).style.display = 'none';
    document.getElementById('edit-' + id).style.display = '';
    document.getElementById('ebtn-' + id).style.display = 'none';
    document.getElementById('sbtn-' + id).style.display = '';
    document.getElementById('edit-' + id).focus();
}

async function saveEdit(id) {
    const name = document.getElementById('edit-' + id).value.trim();
    if (!name) {
        toast('名称不能为空', 'warning');
        return;
    }

    const res = await api(API + '/' + id, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name }),
    });

    if (res.error) {
        toast(res.error, 'error');
        return;
    }

    toast('更新成功', 'success');
    loadItems();
}

async function deleteItem(id) {
    confirmModal({
        title: '确认删除',
        message: '删除后将无法恢复，确定要删除这条记录吗？',
        confirmText: '删除',
        onConfirm: async () => {
            const res = await api(API + '/' + id, { method: 'DELETE' });
            if (res.error) {
                toast(res.error, 'error');
                return;
            }
            toast('删除成功', 'success');
            loadItems();
        },
    });
}

// ==================== Settings ====================

async function loadSettings() {
    const settings = await api('/api/settings');
    for (const [key, value] of Object.entries(settings)) {
        const el = document.getElementById('setting-' + key);
        if (el) el.checked = value === "true" || value === true;
    }
}

async function saveSetting(key, checked) {
    const res = await api('/api/settings', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ [key]: String(checked) }),
    });
    if (res.error) {
        toast(res.error, 'error');
        loadSettings();
        return;
    }
    toast('设置已保存', 'success');
}

// ==================== 初始化 ====================
document.addEventListener('DOMContentLoaded', function() {
    initControl();
    loadItems();
    loadSettings();
});

// 点击二维码弹窗外部关闭
document.addEventListener('click', function(e) {
    const modal = document.getElementById('qrModal');
    if (e.target === modal) {
        closeQrModal();
    }
});
