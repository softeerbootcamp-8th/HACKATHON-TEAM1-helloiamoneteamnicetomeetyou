/*
  어드민 화면의 손맛만 담당한다.

  화면은 서버가 그려서 내려보내기 때문에 여기서 상태를 들고 있지 않는다. 누르는 느낌, 확인
  받기, 토스트 지우기처럼 브라우저에서만 할 수 있는 일만 있다.
*/
(function () {
  'use strict';

  /* ------------------------------------------------------------------
     목록이 순서대로 뜨게 한다.

     CSS 만으로는 몇 번째 줄인지 알 수 없어서 지연 시간을 여기서 넣는다. 줄이 많으면
     마지막 줄이 한참 뒤에 뜨므로 여덟 번째부터는 더 늦추지 않는다.
     ------------------------------------------------------------------ */
  function stagger() {
    document.querySelectorAll('.rows, .demand-list').forEach(function (list) {
      var items = list.querySelectorAll(':scope > .row, :scope > .demand');
      items.forEach(function (item, index) {
        item.style.animationDelay = Math.min(index, 8) * 28 + 'ms';
      });
    });
  }

  /* ------------------------------------------------------------------
     토스트

     스스로 사라지고 흐름을 막지 않는다. 사라지는 애니메이션이 끝난 뒤에 지워야
     화면에서 툭 끊기지 않는다.
     ------------------------------------------------------------------ */
  function toast() {
    var el = document.querySelector('.toast');
    if (!el) return;

    setTimeout(function () {
      el.classList.add('gone');
      el.addEventListener('animationend', function () {
        el.remove();
      });
    }, 2600);
  }

  /* ------------------------------------------------------------------
     되돌릴 수 없는 동작은 한 번 물어본다.

     `data-confirm` 이 붙은 폼은 제출 전에 다이얼로그를 띄운다. window.confirm 을 쓰지
     않는 것은, 브라우저 기본 대화상자가 화면과 따로 놀아서 시연 중에 눈에 띄게 어색하기
     때문이다.
     ------------------------------------------------------------------ */
  function confirmDialog(message) {
    return new Promise(function (resolve) {
      var dialog = document.getElementById('confirm-dialog');
      if (!dialog) {
        resolve(true);
        return;
      }

      dialog.querySelector('[data-confirm-message]').textContent = message;

      function close(ok) {
        dialog.removeEventListener('close', onClose);
        dialog.close();
        resolve(ok);
      }

      function onClose() {
        resolve(false);
      }

      dialog.querySelector('[data-confirm-ok]').onclick = function () {
        close(true);
      };
      dialog.querySelector('[data-confirm-cancel]').onclick = function () {
        close(false);
      };

      // 바깥을 눌러도 닫힌다. Esc 는 dialog 가 알아서 처리한다.
      dialog.onclick = function (event) {
        if (event.target === dialog) close(false);
      };

      dialog.addEventListener('close', onClose);
      dialog.showModal();
    });
  }

  function guardForms() {
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
      form.addEventListener('submit', function (event) {
        if (form.dataset.confirmed === 'yes') return;

        event.preventDefault();
        confirmDialog(form.dataset.confirm).then(function (ok) {
          if (!ok) return;
          form.dataset.confirmed = 'yes';
          form.requestSubmit();
        });
      });
    });
  }

  /* ------------------------------------------------------------------
     같은 폼을 두 번 보내지 않게 한다.

     부스에서 반응이 늦으면 한 번 더 누르게 되는데, 그러면 카드가 두 장 붙거나 더미가
     두 명 생긴다. 누른 순간 버튼을 잠근다.
     ------------------------------------------------------------------ */
  function lockOnSubmit() {
    document.querySelectorAll('form').forEach(function (form) {
      form.addEventListener('submit', function () {
        if (form.dataset.confirm && form.dataset.confirmed !== 'yes') return;

        var button = form.querySelector('button[type="submit"], button:not([type])');
        if (!button) return;

        setTimeout(function () {
          button.disabled = true;
        }, 0);
      });
    });
  }

  /* ------------------------------------------------------------------
     수량 스테퍼. 눌러서 바꾸고 폼을 바로 보낸다.
     ------------------------------------------------------------------ */
  function steppers() {
    document.querySelectorAll('[data-step]').forEach(function (button) {
      button.addEventListener('click', function () {
        var form = button.closest('form');
        var input = form.querySelector('input[name="quantity"]');
        var next = Number(input.value) + Number(button.dataset.step);

        if (next < 1) return;
        input.value = next;
        form.requestSubmit();
      });
    });
  }

  /* ------------------------------------------------------------------
     줄을 통째로 누르면 링크로 간다.

     행 안에 버튼이 있어서 <a> 로 감쌀 수 없다. 버튼을 누른 것인지 빈 곳을 누른 것인지
     여기서 가른다.
     ------------------------------------------------------------------ */
  function linkedRows() {
    document.querySelectorAll('.row.linked[data-href]').forEach(function (row) {
      row.style.cursor = 'pointer';
      row.addEventListener('click', function (event) {
        if (event.target.closest('button, a, input, select, form')) return;
        window.location.href = row.dataset.href;
      });
    });
  }

  /* ------------------------------------------------------------------
     지금 붙어 있는 서버 주소를 적는다.

     로컬과 배포가 같은 화면이라 어느 쪽 데이터를 고치고 있는지 화면만으로는 알 수 없다.
     ------------------------------------------------------------------ */
  function origin() {
    document.querySelectorAll('[data-origin]').forEach(function (el) {
      el.textContent = window.location.origin;
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    origin();
    stagger();
    toast();
    guardForms();
    lockOnSubmit();
    steppers();
    linkedRows();
  });
})();
