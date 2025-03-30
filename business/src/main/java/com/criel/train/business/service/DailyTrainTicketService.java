package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.criel.train.business.domain.generated.DailyTrain;
import com.criel.train.business.domain.generated.TrainStation;
import com.criel.train.business.enumeration.SeatTypeEnum;
import com.criel.train.business.enumeration.TrainTypeEnum;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.business.domain.generated.DailyTrainTicket;
import com.criel.train.business.domain.generated.DailyTrainTicketExample;
import com.criel.train.business.mapper.DailyTrainTicketMapper;
import com.criel.train.business.req.DailyTrainTicketQueryReq;
import com.criel.train.business.req.DailyTrainTicketSaveReq;
import com.criel.train.business.resp.DailyTrainTicketQueryResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class DailyTrainTicketService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainTicketService.class);

    @Autowired
    private DailyTrainTicketMapper dailyTrainTicketMapper;

    @Autowired
    private TrainStationService trainStationService;

    @Autowired
    private DailyTrainSeatService dailyTrainSeatService;

    public void save(DailyTrainTicketSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainTicket dailyTrainTicket = BeanUtil.copyProperties(req, DailyTrainTicket.class);
        if (ObjectUtil.isNull(dailyTrainTicket.getId())) {
            dailyTrainTicket.setId(SnowflakeUtil.getSnowflakeNextId());
            dailyTrainTicket.setCreateTime(now);
            dailyTrainTicket.setUpdateTime(now);
            dailyTrainTicketMapper.insert(dailyTrainTicket);
        } else {
            dailyTrainTicket.setUpdateTime(now);
            dailyTrainTicketMapper.updateByPrimaryKey(dailyTrainTicket);
        }
    }

    public PageResp<DailyTrainTicketQueryResp> queryList(DailyTrainTicketQueryReq req) {
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.setOrderByClause("id desc");
        DailyTrainTicketExample.Criteria criteria = dailyTrainTicketExample.createCriteria();

        LOG.info("DailyTrainTicketQueryReq: {}", req.toString());

        if (req.getDate() != null) {
            criteria.andDateEqualTo(req.getDate());
        }
        if (req.getTrainCode() != null && !req.getTrainCode().isEmpty()) {
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }
        if (req.getStart() != null && !req.getStart().isEmpty()) {
            criteria.andStartEqualTo(req.getStart());
        }
        if (req.getEnd() != null && !req.getEnd().isEmpty()) {
            criteria.andEndEqualTo(req.getEnd());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainTicket> dailyTrainTicketList = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);

        PageInfo<DailyTrainTicket> pageInfo = new PageInfo<>(dailyTrainTicketList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainTicketQueryResp> list = BeanUtil.copyToList(dailyTrainTicketList, DailyTrainTicketQueryResp.class);

        PageResp<DailyTrainTicketQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainTicketMapper.deleteByPrimaryKey(id);
    }

    /**
     * 生成对应日期、对应车次的余票数据
     *
     * @param date
     * @param dailyTrain
     */
    @Transactional
    public void genDaily(Date date, DailyTrain dailyTrain) {
        String trainCode = dailyTrain.getCode();
        String trainType = dailyTrain.getType();

        // 删除原有数据
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        dailyTrainTicketMapper.deleteByExample(dailyTrainTicketExample);

        // 生成
        LOG.info("开始生成{}的{}车次的余票信息", DateUtil.formatDate(date), trainCode);
        Date now = new Date();

        // 生成车站区间信息
        List<TrainStation> trainStationList = trainStationService.selectByTrainCode(trainCode);
        if (trainStationList == null || trainStationList.isEmpty()) {
            LOG.info("{}车次没有车站信息，无法生成每日余票数据", trainCode);
            return;
        }

        // 获取该车次该日期，各座位类型的总数
        int ydzCount = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.YDZ.getCode());
        int edzCount = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.EDZ.getCode());
        int rwCount = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.RW.getCode());
        int ywCount = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.YW.getCode());

        // 车次类型票价系数
        BigDecimal trainTypePriceRate = TrainTypeEnum.getPriceFactor(trainType);

        for (int start = 0; start < trainStationList.size(); start++) {
            TrainStation startStation = trainStationList.get(start);
            // start到end车站区间距离
            BigDecimal distance = BigDecimal.ZERO;
            for (int end = start + 1; end < trainStationList.size(); end++) {
                TrainStation endStation = trainStationList.get(end);
                DailyTrainTicket dailyTrainTicket = new DailyTrainTicket();
                dailyTrainTicket.setId(SnowflakeUtil.getSnowflakeNextId());
                dailyTrainTicket.setDate(date);
                dailyTrainTicket.setTrainCode(trainCode);
                dailyTrainTicket.setStart(startStation.getName());
                dailyTrainTicket.setStartPinyin(startStation.getNamePinyin());
                dailyTrainTicket.setStartTime(startStation.getOutTime());
                dailyTrainTicket.setStartIndex(startStation.getIndex());
                dailyTrainTicket.setEnd(endStation.getName());
                dailyTrainTicket.setEndPinyin(endStation.getNamePinyin());
                dailyTrainTicket.setEndTime(endStation.getInTime());
                dailyTrainTicket.setEndIndex(endStation.getIndex());
                dailyTrainTicket.setCreateTime(now);
                dailyTrainTicket.setUpdateTime(now);
                dailyTrainTicket.setYdz(ydzCount);
                dailyTrainTicket.setEdz(edzCount);
                dailyTrainTicket.setRw(rwCount);
                dailyTrainTicket.setYw(ywCount);

                // 票价 = 车站区间距离 * 座位类型价格 * 车辆类型价格系数 （简化，无阶梯计价）
                distance = distance.add(endStation.getKm()); // getKm()是上一站到本站的距离
                BigDecimal ydzPrice = distance.multiply(SeatTypeEnum.YDZ.getPrice()).multiply(trainTypePriceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal edzPrice = distance.multiply(SeatTypeEnum.EDZ.getPrice()).multiply(trainTypePriceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal rwPrice = distance.multiply(SeatTypeEnum.RW.getPrice()).multiply(trainTypePriceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal ywPrice = distance.multiply(SeatTypeEnum.YW.getPrice()).multiply(trainTypePriceRate).setScale(2, RoundingMode.HALF_UP);
                // 除去没有的座位类型
                if (ydzCount == -1) {
                    ydzPrice = BigDecimal.ZERO;
                }
                if (edzCount == -1) {
                    edzPrice = BigDecimal.ZERO;
                }
                if (rwCount == -1) {
                    rwPrice = BigDecimal.ZERO;
                }
                if (ywCount == -1) {
                    ywPrice = BigDecimal.ZERO;
                }
                dailyTrainTicket.setYdzPrice(ydzPrice);
                dailyTrainTicket.setEdzPrice(edzPrice);
                dailyTrainTicket.setRwPrice(rwPrice);
                dailyTrainTicket.setYwPrice(ywPrice);

                dailyTrainTicketMapper.insert(dailyTrainTicket);
            }
        }

        LOG.info("结束生成{}的{}车次的余票信息", DateUtil.formatDate(date), trainCode);
    }

    /**
     * 根据唯一键查询
     * @param date
     * @param trainCode
     * @param start
     * @param end
     * @return
     */
    public DailyTrainTicket selectByUnique(Date date, String trainCode, String start, String end) {
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.createCriteria()
                .andDateEqualTo(date)
                .andTrainCodeEqualTo(trainCode)
                .andStartEqualTo(start)
                .andEndEqualTo(end);
        List<DailyTrainTicket> dailyTrainTicketList = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);
        if (!dailyTrainTicketList.isEmpty()) {
            return dailyTrainTicketList.get(0);
        } else {
            return null;
        }
    }
}
