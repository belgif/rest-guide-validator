package io.github.belgif.rest.guide.validator;

import lombok.Data;

@Data
public class LineRangePath implements Comparable<LineRangePath> {
    String path;
    int start;
    int end;

    public LineRangePath(String path, int start){
        this.path = path;
        this.start = start;
    }

    @Override
    public int compareTo(LineRangePath o) {
        return Integer.compare(start, o.getStart());
    }

    public boolean inRange(int number){
        return start <= number && number <= end;
    }
}
