/**
 * 매칭 알고리즘(이슈 #20)이 붙기 전까지만 쓰는 임시 코드다.
 *
 * <p><b>이 디렉터리 하나만 지우면 원복된다.</b> 여기 밖의 코드는 이 패키지를 참조하지 않는다.
 * 매칭이 서버로 들어오면 그쪽에서 {@code ExchangeService.create(...)} 를 부르게 되고, 화면이
 * 교환을 직접 만드는 아래 엔드포인트는 필요 없어진다.
 *
 * <p>{@code domain/matching} 패키지를 일부러 만들지 않았다. 매칭 담당자가 새로 만들 자리라,
 * 미리 만들어 두면 파일이 겹친다.
 */
package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.demo;
