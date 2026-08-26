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
     타일에 붙은 장수.

     고른 카드에만 칸을 열어 준다. 안 고른 타일의 칸은 disabled 로 둬서 폼에 실리지
     않게 하는데, 그래야 서버가 받는 수량 목록이 고른 카드와 같은 순서, 같은 길이가 된다.
     체크박스만으로는 무엇을 골랐는지가 오고 수량은 안 오기 때문에 둘을 이렇게 맞춘다.

     타일이 label 이라 안에서 누르는 것이 전부 체크를 뒤집을 수 있다. 수량을 만지려다
     골라 둔 카드가 풀리면 안 되므로 여기서 이벤트를 잡아 둔다.
     ------------------------------------------------------------------ */
  function tileQuantities() {
    document.querySelectorAll('.tile-qty').forEach(function (box) {
      var tile = box.closest('.tile');
      var pick = tile.querySelector('input[type="checkbox"]');
      var input = box.querySelector('input[type="number"]');

      pick.addEventListener('change', function () {
        input.disabled = !pick.checked;
        if (!pick.checked) input.value = '1';
      });

      box.addEventListener('click', function (event) {
        var button = event.target.closest('[data-tile-step]');
        if (button) {
          var next = Number(input.value) + Number(button.dataset.tileStep);
          if (next >= 1) input.value = next;
        }

        // 타일(label)까지 올라가면 카드 선택이 뒤집힌다.
        event.preventDefault();
        event.stopPropagation();
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

  /* ------------------------------------------------------------------
     카드 검색.

     브라우저에서만 거른다. 서버를 다시 부르면 고르던 체크가 초기화되는데, 카드를 몇 장
     골라 둔 상태에서 검색하는 것이 정상적인 순서라 그러면 쓸 수가 없다.

     이미 고른 것은 검색어에 안 걸려도 남긴다. 안 보이는 채로 폼에 실려 나가면 무엇을
     보냈는지 알 수 없게 된다.
     ------------------------------------------------------------------ */
  function cardSearch() {
    document.querySelectorAll('[data-filter]').forEach(function (input) {
      var grid = document.querySelector(input.dataset.filter);
      if (!grid) return;

      input.addEventListener('input', function () {
        var q = input.value.trim().toLowerCase();

        grid.querySelectorAll('.tile').forEach(function (tile) {
          var checked = tile.querySelector('input').checked;
          var hit = !q || (tile.dataset.name || '').indexOf(q) !== -1;
          tile.classList.toggle('hidden', !hit && !checked);
        });
      });
    });
  }

  /* ------------------------------------------------------------------
     카드 이미지가 깨지면 약칭으로 되돌린다.

     주소는 운영자가 손으로 넣는 값이라 오타나 만료된 링크가 들어올 수 있고, 부스 네트워크가
     흔들리면 멀쩡한 주소도 안 뜬다. 그때 깨진 이미지 아이콘이 남으면 무엇을 가리키는 카드인지
     알 수 없게 된다.

     약칭은 이미지 아래에 늘 깔려 있다. 이미지를 걷어내면 그대로 드러난다.

     load 이벤트를 기다리지 않고 error 만 보는 것은, 이미 캐시에서 뜬 이미지는 이 스크립트가
     붙기 전에 로드가 끝나 있기 때문이다. 그런 경우까지 잡으려고 complete 를 함께 본다.
     ------------------------------------------------------------------ */
  function imageFallback() {
    function drop(img) {
      img.remove();
    }

    document.querySelectorAll('.chip-card img, .tile-face img').forEach(function (img) {
      if (img.complete && img.naturalWidth === 0) {
        drop(img);
        return;
      }
      img.addEventListener('error', function () {
        drop(img);
      });
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    imageFallback();
    origin();
    cardSearch();
    stagger();
    toast();
    guardForms();
    lockOnSubmit();
    steppers();
    tileQuantities();
    linkedRows();
  });
})();
