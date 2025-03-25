package com.criel.train.business.domain.generated;

import lombok.Data;

import java.util.Date;

@Data
public class TrainSeat {
    private Long id;

    private String trainCode;

    private Integer carriageIndex;

    private String row;

    private String col;

    private String seatType;

    // 车厢内的座序
    private Integer carriageSeatIndex;

    private Date createTime;

    private Date updateTime;

    public TrainSeat(Long id, String trainCode, Integer carriageIndex, String row, String col, String seatType, Integer carriageSeatIndex, Date createTime, Date updateTime) {
        this.id = id;
        this.trainCode = trainCode;
        this.carriageIndex = carriageIndex;
        this.row = row;
        this.col = col;
        this.seatType = seatType;
        this.carriageSeatIndex = carriageSeatIndex;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public TrainSeat() {}
}