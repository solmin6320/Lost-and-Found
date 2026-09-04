package com.example.lostandfound.repository;

import com.example.lostandfound.dto.request.PostSearchCondition;
import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PostCategory;
import com.example.lostandfound.entity.PostStatus;
import com.example.lostandfound.entity.PostType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

import static com.example.lostandfound.entity.QMember.member;
import static com.example.lostandfound.entity.QPost.post;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> search(PostSearchCondition condition, Pageable pageable) {

        // 목록과 COUNT가 공유
        BooleanExpression[] conditions = {
                keywordContains(condition.keyword()),
                typeEq(condition.type()),
                categoryEq(condition.category()),
                statusEq(condition.status()),
                locationContains(condition.location()),
                lostFoundDateGoe(condition.from()),
                lostFoundDateLoe(condition.to())
        };

        List<Post> content = queryFactory
                .selectFrom(post)
                .join(post.member, member).fetchJoin() // 컬렉션은 조인 금지

                .where(conditions)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset()) // QueryDSL은 직접 붙어야 함

                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
                .where(conditions);

        // 불필요하면 COUNT 실행을 생략
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // or 체이닝은 null 무시가 안 됨
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return
                post.title.contains(keyword).or(post.content.contains(keyword));
    }

    private BooleanExpression typeEq(PostType type) {
        return type == null ? null : post.type.eq(type);
    }

    private BooleanExpression categoryEq(PostCategory category) {
        return category == null ? null : post.category.eq(category);
    }

    private BooleanExpression statusEq(PostStatus status) {
        return status == null ? null : post.status.eq(status);
    }

    private BooleanExpression locationContains(String location) {
        return StringUtils.hasText(location) ? post.location.contains(location) : null;
    }

    private BooleanExpression lostFoundDateGoe(LocalDate from) {
        return from == null ? null : post.lostFoundDate.goe(from);
    }

    private BooleanExpression lostFoundDateLoe(LocalDate to) {
        return to == null ? null : post.lostFoundDate.loe(to);
    }
}
