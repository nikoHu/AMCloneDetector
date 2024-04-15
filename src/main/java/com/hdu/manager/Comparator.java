package com.hdu.manager;

import com.hdu.bean.Measure;
import com.hdu.bean.Pair;

import java.util.List;

public interface Comparator {

    List<Pair> findPairs(List<Measure> measureList);
}
