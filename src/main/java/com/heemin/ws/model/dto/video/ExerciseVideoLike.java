package com.heemin.ws.model.dto.video;

import com.heemin.ws.model.dto.member.Member;
import java.time.LocalDate;

public class ExerciseVideoLike {
    private long id;
    private Member member;
    private ExerciseVideo exerciseVideo;
    private LocalDate regDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public ExerciseVideo getExerciseVideo() {
        return exerciseVideo;
    }

    public void setExerciseVideo(ExerciseVideo exerciseVideo) {
        this.exerciseVideo = exerciseVideo;
    }

    public LocalDate getRegDate() {
        return regDate;
    }

    public void setRegDate(LocalDate regDate) {
        this.regDate = regDate;
    }
}
