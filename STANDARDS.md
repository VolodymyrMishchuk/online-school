# Стандарт Вови (Vova's Standard)

Це офіційний стандарт дизайну інтерфейсу (UI/UX) для проєкту Online School. Всі нові форми та сторінки повинні відповідати цьому стилю.

## 🎨 Кольорова Палітра (Design Tokens)

Визначено в `frontend/src/index.css` (`@theme`):

| Token | Hex | Опис |
|-------|-----|------|
| `--color-brand-primary` | `#F19F97` | Основний акцентний колір (кнопки, активні елементи) |
| `--color-brand-secondary` | `#FF8562` | Колір при наведенні (hover) |
| `--color-brand-light` | `#FFDFDC` | Світлий фон, тіні, фокус |
| `--color-brand-dark` | `#1C1C1C` | Текст заголовків |
| `--color-brand-white` | `#FFFFFF` | Чистий білий |

## 🖼️ Фон та Лейаут

**Глобальний фон:**
```css
body {
    background-color: var(--color-brand-light);
    background-image:
        radial-gradient(at 0% 0%, hsla(11, 82%, 87%, 1) 0px, transparent 50%),
        radial-gradient(at 50% 0%, hsla(5, 77%, 76%, 1) 0px, transparent 50%),
        radial-gradient(at 100% 0%, hsla(11, 82%, 87%, 1) 0px, transparent 50%);
    background-attachment: fixed;
    background-size: cover;
}
```

## 💎 Glassmorphism (Скляний ефект)

Використовувати CSS клас `.glass-panel` для контейнерів форм.

```css
.glass-panel {
    background: rgba(255, 255, 255, 0.65);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.5);
    box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
    border-radius: 0.5rem; /* rounded-lg */
}
```

## 📝 Форми та Інпути

**Контейнер інпута:**
*   Лейбл з іконкою (Lucide React icons, 16px/w-4 h-4).
*   Відступи: `mb-2`, `gap-2`.

**Стиль інпута (Tailwind):**
```tsx
<input 
    className="w-full px-4 py-3 rounded-lg border border-gray-200 
               bg-white/50 outline-none transition-all 
               focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
/>
```
*   Стан спокою: напівпрозорий білий (`bg-white/50`).
*   Фокус: повний білий, рожеве кільце (`ring-brand-light`), рожевий бордер.

## 🔘 Кнопки

**Primary Button:**
```tsx
<button className="flex items-center gap-2 px-6 py-3 bg-brand-primary text-white font-bold rounded-lg 
                   hover:bg-brand-secondary transition-colors shadow-lg hover:shadow-xl 
                   transform active:scale-95 duration-200 disabled:opacity-70">
    <Icon className="w-5 h-5" />
    Текст кнопки
</button>
```
*   Ефекти: тінь, скейл при кліку, зміна кольору при наведенні.

**Secondary/Action Button:**
```tsx
<button className="w-full px-4 py-3 text-sm rounded-lg bg-white text-brand-primary font-bold 
                   hover:bg-brand-primary hover:text-white transition-colors shadow-sm">
    Текст
</button>
```

## 📐 Типографіка

*   Шрифт: `Roboto`.
*   Заголовки сторінок: `text-3xl font-bold text-brand-dark`.
*   Заголовки секцій: `text-xl font-bold text-brand-dark`.

## 🇺🇦 Мова та Локалізація

*   **Мова інтерфейсу:** Виключно **Українська**.
*   Всі тексти, лейбли, повідомлення про помилки мають бути українською мовою.
*   **Комунікація з ШІ (AI Assistant):** Усі діалоги, пояснення коду та відповіді від асистента мають бути виключно **українською мовою**.

## 🔲 Заокруглення (Radius)

*   **Стандарт:** `rounded-lg` (0.5rem / 8px).
*   Цей радіус використовується для **всіх** елементів:
    *   Кнопки
    *   Поля вводу (Inputs)
    *   Картки (Cards)
    *   Модальні вікна (Modals)
    *   Контейнери (`.glass-panel`)

## 🧩 Модальні вікна (Modals)

**Структура:**
1.  **Overlay (Фон):** `fixed inset-0 bg-white/30 backdrop-blur-md` (світлий, розмитий).
2.  **Контейнер:**
    *   Клас: `.glass-panel`
    *   Стиль: `rounded-lg shadow-xl`
    *   Фон: `rgba(255, 255, 255, 0.9)` (майже непрозорий білий, щільніший за звичайні панелі).
    *   Анімація: `animate-in zoom-in-95 duration-200`.
    *   Анімація: `animate-in zoom-in-95 duration-200`.
3.  **Хедер (Header Bar) - СТАТИЧНИЙ:**
    *   Розміщення: `fixed` (relative to flex parent) -> `shrink-0`.
    *   Стиль: `bg-white` (чисто білий фон для виділення).
    *   Бордер: `border-b border-gray-100`.
    *   Тінь: `shadow-sm` (легка тінь).
4.  **Тіло (Body) - СКРОЛ:**
    *   Стиль: `flex-1 overflow-y-auto custom-scrollbar`.
    *   Контент повинен скролитись всередині, шапка і футер залишаються на місці.
5.  **Футер (Footer) - СТАТИЧНИЙ:**
    *   Розміщення: `shrink-0 border-t border-gray-100 bg-white/50 backdrop-blur-sm`.
    *   Кнопки дій (Скасувати/Зберегти) розміщуються тут.

## 📦 Картки та Контейнери (Cards & Containers)

**Загальний стиль карток:**
*   **Радіус:** `rounded-lg` (більш стримані кути).
*   **Відступи (Padding):** `p-5` (компактніше).
*   **Тінь:** `shadow-sm` (легка тінь), `hover:shadow-lg` (при наведенні).
*   **Бордер:** `border border-gray-100`.
*   **Фон:** `bg-white`.

**Блоки фільтрів та налаштувань:**
*   **Радіус:** `rounded-lg`.
*   **Відступи:** `p-3`.
*   **Фон:** `bg-white`.

**Списки (Lists of Cards):**
*   **Відступ між елементами:** `space-y-2` (щільне розташування).

## 🔢 Форматування даних (Data Formatting)

**Плюралізація (Множина):**
Для слів **модуль, урок, файл** використовуємо наступну логіку відмінювання:
*   Число закінчується на `1` (крім `11`) -> однина (наприклад: `1 модуль`, `21 урок`, `31 файл`).
*   Число закінчується на `2, 3, 4` (крім `12, 13, 14`) -> множина, називний відмінок (наприклад: `2 модулі`, `23 уроки`, `4 файли`).
*   Решта випадків (`5-20`, `25-30` тощо) -> множина, родовий відмінок (наприклад: `5 модулів`, `10 уроків`, `25 файлів`).

**Тривалість (Duration):**
*   Менше 1 години -> відображаємо лище хвилини (наприклад: `47 хв`).
*   1 година і більше -> відображаємо години та хвилини, скорочення "год" (наприклад: `1 год 15 хв`, `2 год`).
