export const chatSessions = [
  { id: 1, title: '영업 보고 작성 문의', updatedAt: '2026-11-07T10:00:00' },
  { id: 2, title: '비품 구매 절차 문의', updatedAt: '2026-11-08T09:30:00' },
];

export const chatMessages = {
  1: [
    {
      id: 1,
      role: 'USER',
      content: '월간 영업보고는 어떻게 작성하나요?',
      createdAt: '2026-11-07T10:00:00',
    },
    {
      id: 2,
      role: 'ASSISTANT',
      content: '월간 영업보고는 거래처별 실적을 먼저 정리한 뒤 팀장 검토를 받아 제출합니다.',
      sources: [
        { docId: 1, title: '월간 영업보고 작성법', page: 3 },
        { docId: 2, title: '영업 보고 양식', page: null },
      ],
      createdAt: '2026-11-07T10:00:05',
    },
  ],
  2: [],
};