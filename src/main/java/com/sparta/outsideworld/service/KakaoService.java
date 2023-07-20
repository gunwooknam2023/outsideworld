package com.sparta.outsideworld.service;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.outsideworld.dto.KakaoUserInfoDto;
import com.sparta.outsideworld.entity.User;
import com.sparta.outsideworld.entity.UserRoleEnum;
import com.sparta.outsideworld.jwt.JwtUtil;
import com.sparta.outsideworld.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "KAKAO Login")
@Service
@RequiredArgsConstructor
public class KakaoService {

	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
	private final RestTemplate restTemplate;
	private final JwtUtil jwtUtil;

	public String kakaoLogin(String code) throws JsonProcessingException {
		// 1. "인가 코드"로 "액세스 토큰" 요청
		String accessToken = getToken(code);

		// 2. 토큰으로 카카오 API 호출 : "액세스 토큰"으로 "카카오 사용자 정보" 가져오기
		KakaoUserInfoDto kakaoUserInfo = getKakaoUserInfo(accessToken);

		// 3. 필요시 회원가입
		User kakaoUser = registerKakaoUserIfNeeded(kakaoUserInfo);

		// 4. JWT 토큰 반환
		String createToken = jwtUtil.createToken(kakaoUser.getUsername(),kakaoUser.getRole());

		return createToken;
	}

	private String getToken(String code) throws JsonProcessingException {
		// 요청 URL 만들기
		URI uri = UriComponentsBuilder
			.fromUriString("https://kauth.kakao.com")
			.path("/oauth/token")
			.encode()
			.build()
			.toUri();

		// HTTP Header 생성
		HttpHeaders headers = new HttpHeaders();
		// Content-type 을 입력함으로 인해 이제부터 내가 대입할 데이터가 Key=Value 타입임을 명시
		headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

		// HttpBody 객체 생성
		// 기본 Map 이 한 key 당 하나의 value 를 저장시키는데 반해 MultiValueMap 은 한 key 당 여러 개의 value 를 저장시킬 수 있다.
		// HttpHeaders 는 MultiValueMap 을 상속한다.
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("client_id", "a980dc3fcc17a75986d402184c8ecc00");
		body.add("redirect_uri", "http://localhost:8080/api/user/kakao/callback");
		body.add("code", code);


		// HttpHeader 와 HttpBody 를 하나의 객체에 담기(HttpEntity = HttpHeader 와 HttpBody 를 포함하는 클래스)
		// kakaoTokenRequest 는 header 값과 body 값을 갖고 있는 entity 가 됨
		RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity
			.post(uri)
			.headers(headers)
			.body(body);

		// HTTP 요청 보내기
		// requestEntity: 요청 본문과 헤더를 포함하는 HttpEntity 개체
		// String.class: 응답이 String 일 것으로 예상됨을 나타내는 응답 본문의 클래스
		ResponseEntity<String> response = restTemplate.exchange(
			requestEntity,
			String.class
		);

		// Gson, Json Simple, ObjectMapper
		// JSON data 를 JAVA 언어로 컨버팅
		JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
		return jsonNode.get("access_token").asText();
	}

	private KakaoUserInfoDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {
		// 요청 URL 만들기
		URI uri = UriComponentsBuilder
			.fromUriString("https://kapi.kakao.com")
			.path("/v2/user/me")
			.encode()
			.build()
			.toUri();

		// HTTP Header 생성
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + accessToken);
		headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

		RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity
			.post(uri)
			.headers(headers)
			.body(new LinkedMultiValueMap<>());

		// HTTP 요청 보내기
		ResponseEntity<String> response = restTemplate.exchange(
			requestEntity,
			String.class
		);

		JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
		Long id = jsonNode.get("id").asLong();
		String email = jsonNode.get("kakao_account")
			.get("email").asText();

		log.info("카카오 사용자 정보: " + id + ", " + email);
		return new KakaoUserInfoDto(id, email);
	}

	private User registerKakaoUserIfNeeded(KakaoUserInfoDto kakaoUserInfo) {
		// DB 에 중복된 Kakao Id 가 있는지 확인
		Long kakaoId = kakaoUserInfo.getId();
		User kakaoUser = userRepository.findByKakaoId(kakaoId).orElse(null);

		if (kakaoUser == null) {
			// 카카오 사용자 email 동일한 email 가진 회원이 있는지 확인
			String kakaoEmail = kakaoUserInfo.getEmail();

			User sameEmailUser = userRepository.findByEmail(kakaoEmail).orElse(null);
			if (sameEmailUser != null) {
				kakaoUser = sameEmailUser;

				// 기존 회원정보에 카카오 Id 추가
				kakaoUser = kakaoUser.kakaoIdUpdate(kakaoId);
			} else {
				// 신규 회원가입
				// Username
				String username = kakaoUserInfo.getEmail()+"_"+kakaoId;

				// password: random UUID
				String password = UUID.randomUUID().toString();
				String encodedPassword = passwordEncoder.encode(password);

				// email: kakao email
				String email = kakaoUserInfo.getEmail();
				String introduction = "안녕하세요, 반갑습니다.";
				UserRoleEnum role = UserRoleEnum.USER;


				kakaoUser = new User(username, encodedPassword, email, kakaoId, introduction, role);
			}

			userRepository.save(kakaoUser);
		}
		return kakaoUser;
	}

}