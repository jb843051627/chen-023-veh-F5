package com.fc.v2.service.impl;

import java.util.Date;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fc.v2.mapper.auto.TVehHandleNoteMapper;
import com.fc.v2.model.auto.TVehHandleNote;

/**
 * 按语段落卷员：每一段在自己的新事务里落卷。
 *
 * 同一秒钟同一来路第二遍挤进来，库上唯一键 (flow_id, stage, visit_no, source_no)
 * 只放头一行；撞键的这一笔单独回滚、静默吞掉，不连累主单事务——
 * 卷上留的还是头一遍那行字，不另起新行。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Service
public class VehHandleNoteRecorder {

    @javax.annotation.Resource
    private TVehHandleNoteMapper vehHandleNoteMapper;

    /**
     * 落一段按语；同站同回同来路已落过则不另起新行。
     * @return true 新立了一段；false 头一行已在，这一遍不添行
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean recordIfAbsent(Long flowId, int stage, int visitNo, String sourceNo,
                                  String text, Date writtenAt, int segOrdinal) {
        TVehHandleNote n = new TVehHandleNote();
        n.setFlowId(flowId);
        n.setStage(stage);
        n.setVisitNo(visitNo);
        n.setSourceNo(sourceNo);
        n.setNoteText(text);
        n.setWrittenAt(writtenAt);
        n.setSegOrdinal(segOrdinal);
        n.setDelFlag(0);
        try {
            vehHandleNoteMapper.insert(n);
            return true;
        } catch (DuplicateKeyException e) {
            // 头一行已经立住：后来这遍不另起新行（只回滚本新事务，不碰主单事务）
            return false;
        }
    }
}
