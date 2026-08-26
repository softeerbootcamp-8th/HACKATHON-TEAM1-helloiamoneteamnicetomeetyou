import { Dialog } from '@/components/ui/Dialog'

/**
 * 거절하기 모달. 시안의 `0. 거절하기 모달` 이다.
 *
 * 찔러보기 신청 수신과 자동 매칭 결과의 "거절하기", 장소 보기와 시간 선택의 "닫기"가
 * 전부 이 모달을 거친다. 한 번 거절하면 약속 데이터가 지워지기 때문에 바로 처리하지 않는다.
 */
export function RejectDialog({
  open,
  onKeep,
  onReject,
}: {
  open: boolean
  /** 되돌아간다. 이전 단계에서 넣은 값은 그대로 둔다. */
  onKeep: () => void
  onReject: () => void
}) {
  return (
    <Dialog
      open={open}
      title="이번 교환은 패스할까요?"
      description="괜찮아요, 새 교환 상대를 찾아드릴게요"
      primary={{ label: '아니요', onClick: onKeep }}
      secondary={{ label: '패스할게요', onClick: onReject }}
      onDismiss={onKeep}
    />
  )
}

/**
 * 교환 파토 확인 모달. 시안의 `15. 교환 파토 확인 모달` 이다.
 * 약속 취소하기와 "상대가 오지 않아요" 가 이 모달을 띄운다.
 */
export function BreakupDialog({
  open,
  onFindNew,
  onKeep,
}: {
  open: boolean
  /** 약속을 접고 다시 매칭을 돌린다. */
  onFindNew: () => void
  onKeep: () => void
}) {
  return (
    <Dialog
      open={open}
      title="새 상대를 찾아볼까요?"
      description="이번 교환은 아쉽지만, 바로 다음 상대를 찾아드릴게요"
      primary={{ label: '네, 찾아주세요', onClick: onFindNew, tone: 'brand' }}
      secondary={{ label: '아니오', onClick: onKeep }}
      onDismiss={onKeep}
    />
  )
}
