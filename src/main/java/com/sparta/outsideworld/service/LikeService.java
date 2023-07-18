package com.sparta.outsideworld.service;

import com.sparta.outsideworld.dto.ApiResponseDto;
import com.sparta.outsideworld.entity.Comment;
import com.sparta.outsideworld.entity.Like;
import com.sparta.outsideworld.entity.Post;
import com.sparta.outsideworld.entity.User;
import com.sparta.outsideworld.repository.CommentRepository;
import com.sparta.outsideworld.repository.LikeRepository;
import com.sparta.outsideworld.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;


    @Transactional
    public ApiResponseDto likePost(Long id, User user) {
        //해당 게시글이 존재하는지 확인
        Post post = postRepository.findById(id).orElseThrow(() -> new NullPointerException("Could Not found blog"));

        //해당 게시글에 좋아요를 누른 아이디인지 체크
        Like checkLike = likeRepository.findByUserIdAndPostId(user.getId(), id);
        if (checkLike != null) {
            return new ApiResponseDto("이미 좋아요를 누른 계정입니다.", HttpStatus.BAD_REQUEST.value());
        } else { // 해당 게시글에 좋아요를 누르지 않은 아이디이면 좋아요 처리
            Like like = new Like(user, post);
            likeRepository.save(like);
        }

        // 블로그 게시글의 좋아요 개수 처리
        post.setLikeCount((long) likeRepository.findByPostIdAndCommentId(id, null).size());

        return new ApiResponseDto("좋아요를 눌렀습니다.", HttpStatus.OK.value());
    }

    //게시글 좋아요 취소 API
    @Transactional
    public ApiResponseDto deleteLikePost(Long id, User user) {
        //해당 게시글이 존재하는지 확인
        Post post = postRepository.findById(id).orElseThrow(() -> new NullPointerException("Could Not found blog"));

        //해당 게시글에 좋아요를 누른 아이디인지 체크
        Like checkLike = likeRepository.findByUserIdAndPostId(user.getId(), id);
        if(checkLike != null) {
            likeRepository.delete(checkLike);
        } else { // 해당 게시글에 좋아요를 누르지 않은 아이디이면 좋아요 처리
            return new ApiResponseDto("아직 좋아요를 누르지 않은 계정입니다.", HttpStatus.BAD_REQUEST.value());
        }

        // 블로그 게시글의 좋아요 개수 처리
        post.setLikeCount((long) likeRepository.findByPostIdAndCommentId(id, null).size());

        return new ApiResponseDto("좋아요를 취소했습니다.", HttpStatus.OK.value());
    }


    // 댓글 좋아요 api
    @Transactional
    public ApiResponseDto likeComment(Long blogId, Long commentId, User user) {
        //해당 게시글, 댓글이 존재하는지 확인
        Comment comment = commentRepository.findByIdAndPostId(commentId, blogId);
        if(comment == null) {
            throw new IllegalArgumentException("해당 댓글이 존재하지 않습니다.");
        }

        // 해당 댓글에 좋아요를 누른 아이디인지 확인
        Like checkLike = likeRepository.findByUserIdAndCommentId(user.getId(), commentId);
        if(checkLike != null) {
            return new ApiResponseDto("이미 좋아요를 누른 계정입니다.", HttpStatus.BAD_REQUEST.value());
        } else { // 해당 댓글에 좋아요를 누르지 않은 아이디면 좋아요 처리
            Like like = new Like(user, comment);
            likeRepository.save(like);
        }

        // 댓글 좋아요 개수 처리
        comment.setLikeCount((long) likeRepository.findByCommentId(commentId).size());

        return new ApiResponseDto("좋아요를 눌렀습니다.", HttpStatus.OK.value());
    }


    // 댓글 좋아요 취소 api
    @Transactional
    public ApiResponseDto deleteLikeComment(Long blogId, Long commentId, User user) {
        //해당 게시글, 댓글이 존재하는지 확인
        Comment comment = commentRepository.findByIdAndPostId(commentId, blogId);
        if(comment == null) {
            throw new IllegalArgumentException("해당 댓글이 존재하지 않습니다.");
        }

        // 해당 댓글에 좋아요를 누른 아이디인지 확인
        Like checkLike = likeRepository.findByUserIdAndCommentId(user.getId(), commentId);
        if(checkLike != null) {
            likeRepository.delete(checkLike);
        } else {
            return new ApiResponseDto("아직 해당 댓글에 좋아요를 누르지 않았습니다.", HttpStatus.BAD_REQUEST.value());
        }

        // 댓글 좋아요 개수 처리
        comment.setLikeCount((long) likeRepository.findByCommentId(commentId).size());

        return new ApiResponseDto("좋아요를 취소했습니다.", HttpStatus.OK.value());
    }
}
