# LeAIrning design system (v0 sketch)

Product: AI learning app. Users upload documents, chat with them, and generate quizzes. Angular + Tailwind frontend.

Style: modern minimalist AI-app (ChatGPT / Claude / Linear feel). Calm, lots of whitespace, flat, almost no shadows, hairline borders. No gradients, no illustrations, no decorative noise.

## Color (light theme; dark theme must be a straight token swap)
- bg: #FAFAFA, surface: #FFFFFF, border: #E5E5E5, border-strong: #D4D4D4
- text: #0A0A0A, text-muted: #737373
- accent (single): #4F46E5 (indigo-600), hover #4338CA, used ONLY for primary button, focus ring, links
- error: #DC2626
- dark: bg #0A0A0A, surface #141414, border #262626, text #FAFAFA, muted #A3A3A3, accent #6366F1

## Type
Inter only. Title 24/600, body 14/400, label 13/500, muted 13/400. Tight letter-spacing on titles.

## Shape & spacing
Radius 10px for inputs/buttons, 16px for the card. 8px spacing grid. Inputs 40px tall, 1px border, focus = 2px accent ring. Primary button full-width 40px, solid accent, white text.

## Layout
Single centered column (max 380px) on bg. Small wordmark "LeAIrning" (text + tiny geometric mark in accent) above the card. Login/Register switch is a segmented control at top of the card. Mobile first, works at 375px.

## Fields (match backend)
- Login: identifier (email or username), password
- Register: name, username, email, password (min 8 chars, show hint)

## App shell (authenticated area)
- Left sidebar 240px, surface bg, 1px right border: wordmark (birrete icon + LeAIrning) at top, nav items (Inicio, Documentos, Chat, Cuestionarios) as 36px rows with 18px monoline icons, active row = subtle bg #F0F0F0 (dark #1C1C1C) with ink text; user row at bottom (initials avatar + name) and a sun/moon theme toggle.
- Main area on bg #FAFAFA, content max-width 960px, 32px padding. Page title 24/600.
- Cards: surface, 1px border, radius 16px, no shadow. Status chips: 12px text, radius 6px, neutral bg; "Listo" uses success tint only for the status dot.
- Logo mark: graduation cap (filled indigo diamond, curved base line, vertical tassel line) with a small 4-point spark at top right, indigo #4F46E5.
