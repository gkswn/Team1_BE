package com.example.team1_be.domain.User;
import com.example.team1_be.domain.User.DTO.Join;
import com.example.team1_be.domain.User.DTO.Login;
import com.example.team1_be.domain.User.UnfinishedUser.UnfinishedUser;
import com.example.team1_be.domain.User.UnfinishedUser.UnfinishedUserRepository;
import com.example.team1_be.utils.errors.exception.BadRequestException;
import com.example.team1_be.utils.errors.exception.NotFoundException;
import com.example.team1_be.utils.security.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final UnfinishedUserRepository unfinishedUserRepository;
    private final JwtProvider jwtProvider;

    @Transactional(noRollbackFor = NotFoundException.class)
    public Login.Response login(String code, Long kakaoId) {
        User user = userRepository.findByKakaoId(kakaoId).orElse(null);
        if (user == null) {
            UnfinishedUser unfinishedUser = UnfinishedUser.builder()
                    .code(code)
                    .kakaoId(kakaoId)
                    .build();
            unfinishedUserRepository.save(unfinishedUser);

            throw new NotFoundException("회원이 아닙니다.");
        }

        return new Login.Response(user.getIsAdmin());
    }

    // login 시도했던 code를 통해, join 시 kakaoId와 매칭
    public Long matchKakaoId(String code) {
        UnfinishedUser unfinishedUser = unfinishedUserRepository.findByCode(code).orElseThrow(
                () -> new BadRequestException("유효하지 않은 code입니다.")
        );
        return unfinishedUser.getKakaoId();
    }

    @Transactional
    public Join.Response join(Join.Request request, Long kakaoId) {
        User user = userRepository.findByKakaoId(kakaoId).orElse(null);
        if (user != null) {
            throw new BadRequestException("이미 가입되었습니다.");
        }

        user = User.builder()
                .kakaoId(kakaoId)
                .name(request.getUserName())
                .phoneNumber(null)
                .isAdmin(request.getIsAdmin())
                .build();
        userRepository.save(user);

        return new Join.Response(request.getIsAdmin());
    }

    public String getJWT(Long kakaoId){
        User user = userRepository.findByKakaoId(kakaoId).orElse(null);
        return jwtProvider.createJwt(user.getId());
    }

}