package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.criel.train.business.domain.generated.Station;
import com.criel.train.business.enumeration.SeatColEnum;
import com.criel.train.common.exception.BusinessException;
import com.criel.train.common.exception.BusinessExceptionEnum;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.business.domain.generated.TrainCarriage;
import com.criel.train.business.domain.generated.TrainCarriageExample;
import com.criel.train.business.mapper.TrainCarriageMapper;
import com.criel.train.business.req.TrainCarriageQueryReq;
import com.criel.train.business.req.TrainCarriageSaveReq;
import com.criel.train.business.resp.TrainCarriageQueryResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainCarriageService {

    private static final Logger LOG = LoggerFactory.getLogger(TrainCarriageService.class);

    @Resource
    private TrainCarriageMapper trainCarriageMapper;

    public void save(TrainCarriageSaveReq req) {
        DateTime now = DateTime.now();
        TrainCarriage trainCarriage = BeanUtil.copyProperties(req, TrainCarriage.class);

        // 由座位类型得到列数，进而得总座位数
        // TODO 硬卧和软卧暂时有bug
        List<SeatColEnum> seatColEnums = SeatColEnum.getColsByType(trainCarriage.getSeatType());
        trainCarriage.setColCount(seatColEnums.size());
        trainCarriage.setSeatCount(trainCarriage.getRowCount() * seatColEnums.size());

        if (ObjectUtil.isNull(trainCarriage.getId())) {
            // 检查唯一键：车次编号 + 车厢序号
            if (selectByUnique(req.getTrainCode(), req.getIndex()) != null) {
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_CARRIAGE_INDEX_UNIQUE_ERROR);
            }

            trainCarriage.setId(SnowflakeUtil.getSnowflakeNextId());
            trainCarriage.setCreateTime(now);
            trainCarriage.setUpdateTime(now);
            trainCarriageMapper.insert(trainCarriage);
        } else {
            trainCarriage.setUpdateTime(now);
            trainCarriageMapper.updateByPrimaryKey(trainCarriage);
        }
    }

    public PageResp<TrainCarriageQueryResp> queryList(TrainCarriageQueryReq req) {
        TrainCarriageExample trainCarriageExample = new TrainCarriageExample();
        trainCarriageExample.setOrderByClause("train_code asc, `index` asc");
        TrainCarriageExample.Criteria criteria = trainCarriageExample.createCriteria();

        // 根据车次编号查询
        if (ObjectUtil.isNotEmpty(req.getTrainCode())) {
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<TrainCarriage> trainCarriageList = trainCarriageMapper.selectByExample(trainCarriageExample);

        PageInfo<TrainCarriage> pageInfo = new PageInfo<>(trainCarriageList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<TrainCarriageQueryResp> list = BeanUtil.copyToList(trainCarriageList, TrainCarriageQueryResp.class);

        PageResp<TrainCarriageQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        trainCarriageMapper.deleteByPrimaryKey(id);
    }

    /**
     * 根据车次编号查询车厢信息
     * @param trainCode
     * @return
     */
    public List<TrainCarriage> selectByTrainCode(String trainCode) {
        TrainCarriageExample trainCarriageExample = new TrainCarriageExample();
        trainCarriageExample.setOrderByClause("`index` asc");
        TrainCarriageExample.Criteria criteria = trainCarriageExample.createCriteria();
        criteria.andTrainCodeEqualTo(trainCode);
        return trainCarriageMapper.selectByExample(trainCarriageExample);
    }

    /**
     * 根据唯一键查询
     * @param trainCode
     * @param index
     * @return
     */
    private TrainCarriage selectByUnique(String trainCode, Integer index) {
        TrainCarriageExample trainCarriageExample = new TrainCarriageExample();
        TrainCarriageExample.Criteria criteria = trainCarriageExample.createCriteria();
        criteria.andTrainCodeEqualTo(trainCode).andIndexEqualTo(index);
        List<TrainCarriage> trainCarriageList = trainCarriageMapper.selectByExample(trainCarriageExample);
        if (!trainCarriageList.isEmpty()) {
            return trainCarriageList.get(0);
        } else {
            return null;
        }
    }
}
