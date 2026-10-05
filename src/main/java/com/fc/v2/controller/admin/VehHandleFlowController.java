package com.fc.v2.controller.admin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TVehHandleFlow;
import com.fc.v2.model.custom.VehHandleBoard;
import com.fc.v2.model.custom.VehHandleOpenForm;
import com.fc.v2.model.custom.VehHandleQuery;
import com.fc.v2.model.custom.VehHandleReplyMsg;
import com.fc.v2.model.custom.VehHandleReplyReceipt;
import com.fc.v2.service.ITVehHandleFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 严重报警处置单 Controller（一张处置单从哪儿起、到哪儿止，控制器只递话）。
 *
 * 这一张走到哪一站、下一站开不开、收口后再递接不接、同站第二遍另不另起行——四问只认
 * 服务层 {@link ITVehHandleFlowService} 与 VehHandleRuler 一处给的答案，此处不另答、不预排站次。
 * 旧的 advance/rollback 口子模样不改：进一个号、还一张单；日后换走法另起一套接，不动它。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "严重报警处置单")
@Controller
@RequestMapping("/vehHandleFlow")
public class VehHandleFlowController extends BaseController {

    private final String prefix = "admin/vehHandleFlow";

    /** 墙钟时刻写法（与卷上落库同一套说法，避开 JDBC 时区错位） */
    private static final String FMT_TS = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private ITVehHandleFlowService vehHandleFlowService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("vehHandleFlow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "严重报警处置单值班台账", action = "board")
    @ApiOperation(value = "值班台账开屏",
            notes = "屏头没收口卷数与名单同一条 SQL 同一回点算；每卷回挂当站与下一站开不开，全照服务层那一把尺")
    @GetMapping("/list")
    @RequiresPermissions("vehHandleFlow:list")
    @ResponseBody
    public ResultTable list(VehHandleQuery query) {
        VehHandleBoard board = vehHandleFlowService.openBoard(query);
        ResultTable table = ResultTable.pageTable(board.getTotalCount(), board.getFlows());
        // 屏头两个数照同一回点算的结果摆，接口层不另点一遍
        table.setHandleTotal(board.getTotalCount());
        table.setHandleActive(board.getActiveCount());
        return table;
    }

    @ApiOperation(value = "卷情回读", notes = "按语段按时刻序挂齐，下一站开不开照系统数出来的当站回挂")
    @GetMapping("/detail/{id}")
    @RequiresPermissions("vehHandleFlow:list")
    @ResponseBody
    public AjaxResult detail(@PathVariable("id") Long id) {
        TVehHandleFlow r = vehHandleFlowService.selectTVehHandleFlowById(id);
        return r == null ? AjaxResult.error("查无这张处置单") : AjaxResult.success(200, "卷情回读", r);
    }

    @Log(title = "严重报警处置单立单", action = "open")
    @ApiOperation(value = "定性立单",
            notes = "车辆营运代号、报警那一刻、当刻那个数（车速/连续驾驶时长）、报警类别四样齐才起；"
                    + "同车同类已有在办卷的第二张起不来；单代号系统排")
    @PostMapping("/open")
    @RequiresPermissions("vehHandleFlow:open")
    @ResponseBody
    public AjaxResult open(VehHandleOpenForm form) {
        return AjaxResult.success(200, "处置单已立", vehHandleFlowService.openFlow(form));
    }

    @Log(title = "严重报警处置单推进", action = "advance")
    @ApiOperation(value = "推进一档", notes = "只许进紧邻站；下一站没开（知会没齐/回告钟点没走满）当没开；收口的推不动")
    @PostMapping("/advance")
    @RequiresPermissions("vehHandleFlow:advance")
    @ResponseBody
    public AjaxResult advance(Long id, String remark) {
        return toAjax(vehHandleFlowService.advance(id, remark) != null ? 1 : 0);
    }

    @Log(title = "严重报警处置单回退", action = "rollback")
    @ApiOperation(value = "回退一档", notes = "一次退一站，被退站攒头清零重攒、旧按语留痕；头一档退无可退；收口的退不动")
    @PostMapping("/rollback")
    @RequiresPermissions("vehHandleFlow:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String remark) {
        return toAjax(vehHandleFlowService.rollback(id, remark) != null ? 1 : 0);
    }

    @Log(title = "知会笔记落卷", action = "notify")
    @ApiOperation(value = "知会站落一笔", notes = "看知会那几笔全没全；末笔齐了当即进回告站起算钟点；已齐再递不另起行")
    @PostMapping("/notify")
    @RequiresPermissions("vehHandleFlow:notify")
    @ResponseBody
    public AjaxResult notifyNote(Long id, String text, String at) {
        TVehHandleFlow r = vehHandleFlowService.addNotifyNote(id, text, parseWallClock(at));
        return r == null ? AjaxResult.error("查无这张处置单") : AjaxResult.success(200, "知会已落", r);
    }

    @Log(title = "企业回告报文收口", action = "reply")
    @ApiOperation(value = "接企业回告报文",
            notes = "报文自报步次只当来意：入库按系统数出来的站记、回读照系统数出来的回；"
                    + "只在回告站接头一遍，同站第二遍不另起行；收口后的不接")
    @PostMapping("/reply")
    @RequiresPermissions("vehHandleFlow:reply")
    @ResponseBody
    public AjaxResult reply(VehHandleReplyMsg msg) {
        VehHandleReplyReceipt rc = vehHandleFlowService.acceptReply(msg);
        return AjaxResult.success(200, rc.getMark() == null ? "回告收口" : rc.getMark(), rc);
    }

    @Log(title = "复查意见落卷入档", action = "review")
    @ApiOperation(value = "归档站落复查意见", notes = "入档只看复查意见落没落；落下即凭复查入档收口封尾")
    @PostMapping("/review")
    @RequiresPermissions("vehHandleFlow:review")
    @ResponseBody
    public AjaxResult review(Long id, String opinion, String at) {
        return AjaxResult.success(200, "复查入档收口", vehHandleFlowService.submitReview(id, opinion, parseWallClock(at)));
    }

    @Log(title = "非运输机构督办收口", action = "closeOutside")
    @ApiOperation(value = "收口两说：不归运输机构督办", notes = "任何在办站都收得，收了即封尾只读")
    @PostMapping("/closeOutside")
    @RequiresPermissions("vehHandleFlow:close")
    @ResponseBody
    public AjaxResult closeOutside(Long id, String reason, String at) {
        return toAjax(vehHandleFlowService.closeOutside(id, reason, parseWallClock(at)) != null ? 1 : 0);
    }

    @Log(title = "业户销户收口", action = "closeDeregistered")
    @ApiOperation(value = "收口两说：业户已销户", notes = "名录上确为销户才收得，收了即封尾只读")
    @PostMapping("/closeDeregistered")
    @RequiresPermissions("vehHandleFlow:close")
    @ResponseBody
    public AjaxResult closeDeregistered(Long id, String reason, String at) {
        return toAjax(vehHandleFlowService.closeDeregistered(id, reason, parseWallClock(at)) != null ? 1 : 0);
    }

    /** 墙钟字符串解析：只按卷上落库同一套墙钟说法解，不交给 JDBC 绑参 */
    private Date parseWallClock(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_TS);
        sdf.setLenient(false);
        try {
            return sdf.parse(text.trim());
        } catch (ParseException e) {
            return null;
        }
    }
}
