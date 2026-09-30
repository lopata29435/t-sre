const form = document.querySelector('#task-form');
const titleInput = document.querySelector('#title');
const descriptionInput = document.querySelector('#description');
const statusInput = document.querySelector('#status');
const submitButton = document.querySelector('#submit');
const cancelButton = document.querySelector('#cancel');
const refreshButton = document.querySelector('#refresh');
const message = document.querySelector('#message');
const taskList = document.querySelector('#tasks');
const empty = document.querySelector('#empty');
const labels = { TODO: 'Нужно сделать', IN_PROGRESS: 'В работе', DONE: 'Готово' };
let tasks = [];
let activeFilter = 'ALL';
let editingId = null;

function notify(text, isError = false) {
    message.textContent = text;
    message.classList.toggle('error', isError);
    message.setAttribute('role', isError ? 'alert' : 'status');
    message.hidden = false;
}

async function api(path = '', options = {}) {
    let response;
    try {
        response = await fetch(`/api/tasks${path}`, {
            cache: 'no-store', ...options,
            headers: { 'Content-Type': 'application/json', ...options.headers }
        });
    } catch {
        throw new Error('Не удалось связаться с сервером. Проверьте соединение и повторите попытку.');
    }
    if (response.status === 204) return null;
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.message || `Ошибка сервера (${response.status})`);
    return data;
}

function resetForm() {
    editingId = null;
    form.reset();
    document.querySelector('#form-heading').textContent = 'Новая задача';
    submitButton.textContent = 'Добавить задачу';
    cancelButton.hidden = true;
}

function editTask(task) {
    editingId = task.id;
    titleInput.value = task.title;
    descriptionInput.value = task.description;
    statusInput.value = task.status;
    document.querySelector('#form-heading').textContent = `Задача #${task.id}`;
    submitButton.textContent = 'Сохранить изменения';
    cancelButton.hidden = false;
    titleInput.focus();
    form.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

async function deleteTask(task, button) {
    if (!window.confirm(`Удалить задачу «${task.title}»?`)) return;
    button.disabled = true;
    try {
        await api(`/${task.id}`, { method: 'DELETE' });
        tasks = tasks.filter(item => item.id !== task.id);
        if (editingId === task.id) resetForm();
        render();
        notify('Задача удалена.');
    } catch (error) {
        notify(error.message, true);
        button.disabled = false;
    }
}

function render() {
    document.querySelector('#total').textContent = tasks.length;
    const done = tasks.filter(task => task.status === 'DONE').length;
    document.querySelector('#progress').textContent = `Готово ${done} из ${tasks.length}`;
    const visibleTasks = tasks.filter(task => activeFilter === 'ALL' || task.status === activeFilter);
    taskList.replaceChildren();
    empty.hidden = visibleTasks.length > 0;
    empty.textContent = tasks.length === 0
        ? 'Пока здесь чистый лист. Добавьте первую задачу.'
        : 'В этом статусе пока нет задач.';

    for (const task of visibleTasks) {
        const card = document.querySelector('#task-template').content.firstElementChild.cloneNode(true);
        card.dataset.status = task.status;
        card.querySelector('.status-badge').textContent = labels[task.status];
        card.querySelector('.task-id').textContent = `#${task.id}`;
        card.querySelector('.task-title').textContent = task.title;
        card.querySelector('.task-description').textContent = task.description;
        card.querySelector('.task-description').hidden = !task.description;
        const time = card.querySelector('time');
        time.dateTime = task.updatedAt;
        time.textContent = `Обновлено ${new Date(task.updatedAt).toLocaleString('ru-RU', {
            day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
        })}`;
        card.querySelector('.edit').addEventListener('click', () => editTask(task));
        const deleteButton = card.querySelector('.delete');
        deleteButton.addEventListener('click', () => deleteTask(task, deleteButton));
        taskList.append(card);
    }
}

async function loadTasks() {
    refreshButton.disabled = true;
    taskList.setAttribute('aria-busy', 'true');
    try {
        tasks = await api();
        render();
        message.hidden = true;
    } catch (error) {
        document.querySelector('#progress').textContent = 'Список не обновлён';
        notify(error.message, true);
    } finally {
        refreshButton.disabled = false;
        taskList.setAttribute('aria-busy', 'false');
    }
}

form.addEventListener('submit', async event => {
    event.preventDefault();
    const title = titleInput.value.trim();
    if (!title) {
        notify('Укажите название задачи.', true);
        titleInput.focus();
        return;
    }
    const id = editingId;
    submitButton.disabled = true;
    cancelButton.disabled = true;
    try {
        const task = await api(id === null ? '' : `/${id}`, {
            method: id === null ? 'POST' : 'PUT',
            body: JSON.stringify({ title, description: descriptionInput.value.trim(), status: statusInput.value })
        });
        tasks = [task, ...tasks.filter(item => item.id !== task.id)].sort((a, b) => b.id - a.id);
        resetForm();
        render();
        notify(id === null ? 'Задача добавлена.' : 'Изменения сохранены.');
    } catch (error) {
        notify(error.message, true);
    } finally {
        submitButton.disabled = false;
        cancelButton.disabled = false;
    }
});

cancelButton.addEventListener('click', resetForm);
refreshButton.addEventListener('click', loadTasks);
for (const button of document.querySelectorAll('[data-filter]')) {
    button.addEventListener('click', () => {
        activeFilter = button.dataset.filter;
        document.querySelectorAll('[data-filter]').forEach(item => {
            item.setAttribute('aria-pressed', String(item === button));
        });
        render();
    });
}
loadTasks();
