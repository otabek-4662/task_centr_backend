-- V39: Kanban ustunlari nomlarini o'zbekona sleng / samimiy iboralar bilan sinxronlashtirish
UPDATE board_columns SET title = 'Dushanbadan' WHERE title IN ('To Do', 'TODO', 'Dushanbadan boshlaymiz');
UPDATE board_columns SET title = 'Jumagacha bitadi' WHERE title IN ('In Progress', 'IN_PROGRESS', 'Qozonda qaynayapti');
UPDATE board_columns SET title = 'Buyuk ishlar boshlanishi' WHERE title IN ('Done', 'DONE', 'Ish bitdi');
