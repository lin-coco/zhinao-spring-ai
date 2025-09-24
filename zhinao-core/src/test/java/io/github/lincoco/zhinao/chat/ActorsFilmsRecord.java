/*
Copyright 2025 lin-coco

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package io.github.lincoco.zhinao.chat;

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
