// 시간은 버리고 "날짜"만 비교하기 위해 자정으로 맞춘 Date를 만든다
const startOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate());

export function groupSessionsByDate(sessions, now = new Date()) {
    const today = startOfDay(now);
    const groups = [
        { label: '오늘', items: [] },
        { label: '어제', items: [] },
        { label: '지난 7일', items: [] },
        { label: '이전', items: [] },
    ];

    for (const s of sessions) {
        const date = new Date(s.updatedAt);

        // 날짜가 잘못된 항목은 '이전'에 넣고 다음 항목으로
        if (Number.isNaN(date.getTime())) {
            groups[3].items.push(s);
            continue;
        }

        // 며칠 전인지: (오늘 자정 - 그 날짜 자정) / 하루의 밀리초
        const diffDays = Math.round((today - startOfDay(date)) / (1000 * 60 * 60 * 24));

        if (diffDays === 0) groups[0].items.push(s);
        else if (diffDays === 1) groups[1].items.push(s);
        else if (diffDays >= 2 && diffDays <= 7) groups[2].items.push(s);
        else groups[3].items.push(s);
    }

    return groups.filter((g) => g.items.length > 0);
}