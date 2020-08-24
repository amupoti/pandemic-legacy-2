package org.amupoti.pandemic.model;

import lombok.Value;
import org.amupoti.pandemic.util.SheetsServiceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value
public class InfectionCardAppearances {

    List<Boolean> appearsInEpidemic;

    public static InfectionCardAppearances fromRow(List<Object> row) {

        //Exclude last column. Last dummy column is needed in order to have all rows with the same number of elements
        int columns = row.size() - 1;
        int firstColumnWithEpidemic = 5;

        List<Boolean> appearances = IntStream
                .range(firstColumnWithEpidemic, columns)
                .mapToObj(index -> SheetsServiceUtil.parseBoolean(row.get(index)))
                .collect(Collectors.toList());

        List<Boolean> totalAppearances = new ArrayList<>();
        //If it's in the infection deck, it's considered first appearance
        totalAppearances.add(Boolean.TRUE);
        totalAppearances.addAll(appearances);
        return new InfectionCardAppearances(totalAppearances);
    }

}