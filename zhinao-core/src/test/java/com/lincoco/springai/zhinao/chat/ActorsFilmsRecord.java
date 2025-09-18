package com.lincoco.springai.zhinao.chat;

import java.util.List;

public record ActorsFilmsRecord(String actor, List<String> movies) {

    @Override
    public String actor() {
        return actor;
    }

    @Override
    public List<String> movies() {
        return movies;
    }

    @Override
    public String toString() {
        return "ActorsFilmsRecord{" +
                "actor='" + actor + '\'' +
                ", movies=" + movies +
                '}';
    }
}
