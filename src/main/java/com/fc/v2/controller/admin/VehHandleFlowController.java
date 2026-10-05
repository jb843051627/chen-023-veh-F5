package com.fc.v2.controller.admin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TVehHandleFlow;
import com.fc.v2.model.custom.VehHandleFlowForm;
import com.fc.v2.model.custom.VehHandleFlowView;
import com.fc.v2.service.ITVehHandleFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 严重报警处置单 Controller——四站流转只此一道门。
 *
 * 走到哪一站、下一站开不开、收过口再递接不接、同站第二遍另不另起新行，
 * 四问的答案只认服务层 {@link ITVehHandleFlowService} 一处；本控制器只递话、解墙钟，
 * 不数一个站次、不定一个门槛。报文里捎来的 claimedStage 原样转交，由服务层当来意看待。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "严重报警处置单")
@Controller
@RequestMapping("/vehHandleFlow")
public class VehHandleFlowController extends BaseController {

    private final String prefix = "admin/vehHandleFlow";

    /** 墙钟写法（与落库、按语同一套，避开 JDBC 时区错位） */
    private static final String FMT_TS = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private ITVehHandleFlowService vehHandleFlowService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("vehHandleFlow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "严重报警处置单流转台账", action = "list")
    @ApiOperation(value = "流转台账", notes = "流转台账")
    @GetMapping("/list")
    @RequiresPermissions("vehHandleFlow:list")
    @ResponseBody
    public ResultTable list(TVehHandleFlow record) {
        QueryWrapper<TVehHandleFlow> queryWrapper = new QueryWrapper<TVehHandleFlow>()
                .orderByDesc("create_time");
        startPage();
        com.github.pagehelper.PageInfo<TVehHandleFlow> page =
                new com.github.pagehelper.PageInfo<TVehHandleFlow>(
                        vehHandleFlowService.selectTVehHandleFlowList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "严重报警处置单立单", action = "open")
    @ApiOperation(value = "定性立单", notes = "定性四样齐才起得来；同车同类同时只容一张在办卷")
    @PostMapping("/open")
    @RequiresPermissions("vehHandleFlow:open")
    @ResponseBody
    public AjaxResult open(VehHandleFlowForm form) {
        // 报警那一刻随表单按全局墙钟绑定；立单时刻取问话当下
        TVehHandleFlow r = vehHandleFlowService.open(form, new Date());
        if (r == null) {
            return error("立不起来：定性四样缺一样、类别不认，或这辆车这类报警已有一张在办的卷");
        }
        return AjaxResult.success(200, "单已立在定性站", r);
    }

    @Log(title = "严重报警处置单敲门", action = "knock")
    @ApiOperation(value = "当站递按语", notes = "同来路第二遍不另起新行；报文步次只当来意")
    @PostMapping("/knock")
    @RequiresPermissions("vehHandleFlow:knock")
    @ResponseBody
    public AjaxResult knock(Long id, VehHandleFlowForm form, String at) {
        AjaxResult bad = badMoment(at);
        if (bad != null) {
            return bad;
        }
        VehHandleFlowView v = vehHandleFlowService.knock(id, form, momentOrNow(at));
        if (v == null) {
            return error("卷不存在或已收口封尾，按语接不下");
        }
        return AjaxResult.success(200, v.getClaimedDiffers() != null && v.getClaimedDiffers()
                ? "按语已落；报文步次与系统数的不是一站，入库照系统数的记" : "按语已落", v);
    }

    @Log(title = "严重报警处置单推进", action = "advance")
    @ApiOperation(value = "进紧邻一站", notes = "跳站不作数；门槛不齐/钟点没走满，下一站开着也当没开")
    @PostMapping("/advance")
    @RequiresPermissions("vehHandleFlow:advance")
    @ResponseBody
    public AjaxResult advance(Long id, Integer claimedStage, String note, String at) {
        AjaxResult bad = badMoment(at);
        if (bad != null) {
            return bad;
        }
        VehHandleFlowView v = vehHandleFlowService.advance(
                id, claimedStage, note, momentOrNow(at));
        if (v == null) {
            return error("进不去：只许进紧邻一站，且当站门槛得齐、回告钟点得走满");
        }
        return AjaxResult.success(200, "已进" + v.getStageName() + "站", v);
    }

    @Log(title = "严重报警处置单回退", action = "rollback")
    @ApiOperation(value = "退一站", notes = "一次只退一站，旧按语压卷末，重走重新攒；头一站退无可退")
    @PostMapping("/rollback")
    @RequiresPermissions("vehHandleFlow:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String note, String at) {
        AjaxResult bad = badMoment(at);
        if (bad != null) {
            return bad;
        }
        VehHandleFlowView v = vehHandleFlowService.rollback(id, note, momentOrNow(at));
        if (v == null) {
            return error("退不了：已在头一站，或单子已收口封尾");
        }
        return AjaxResult.success(200, "已退回" + v.getStageName() + "站", v);
    }

    @Log(title = "严重报警处置单特殊收口", action = "closeSpecial")
    @ApiOperation(value = "非督办/销户收口", notes = "不归运输机构督办、业户已销户两说也算收口")
    @PostMapping("/closeSpecial")
    @RequiresPermissions("vehHandleFlow:close")
    @ResponseBody
    public AjaxResult closeSpecial(Long id, String closeKind, String note, String at) {
        AjaxResult bad = badMoment(at);
        if (bad != null) {
            return bad;
        }
        VehHandleFlowView v = vehHandleFlowService.closeSpecial(
                id, closeKind, note, momentOrNow(at));
        if (v == null) {
            return error("收不了口：名目只认 notTransport/cancelled，销户须照业户总名录核得销户");
        }
        return AjaxResult.success(200, "已按「" + v.getCloseKindName() + "」收口封尾", v);
    }

    @Log(title = "严重报警处置单复查入档", action = "archive")
    @ApiOperation(value = "复查入档", notes = "回告钟点走满、复查意见落格才入档，入档即封尾")
    @PostMapping("/archive")
    @RequiresPermissions("vehHandleFlow:archive")
    @ResponseBody
    public AjaxResult archive(Long id, String reviewOpinion, String note, String at) {
        AjaxResult bad = badMoment(at);
        if (bad != null) {
            return bad;
        }
        VehHandleFlowView v = vehHandleFlowService.archive(
                id, reviewOpinion, note, momentOrNow(at));
        if (v == null) {
            return error("入不了档：须已到回告之后、钟点走满，且复查意见落格");
        }
        return AjaxResult.success(200, "复查意见已落，归档封尾", v);
    }

    @ApiOperation(value = "单子回读", notes = "册面与回读同一份答案：四问只认这一处")
    @GetMapping("/detail")
    @RequiresPermissions("vehHandleFlow:view")
    @ResponseBody
    public AjaxResult detail(Long id, String at) {
        AjaxResult bad = badMoment(at);
        if (bad != null) {
            return bad;
        }
        VehHandleFlowView v = vehHandleFlowService.getView(id, momentOrNow(at));
        if (v == null) {
            return error("没有这张卷");
        }
        return AjaxResult.success(200, "回读", v);
    }

    @ApiOperation(value = "在办张数", notes = "屏上那个数：回库一站一站点出来的同一个数")
    @GetMapping("/openCount")
    @RequiresPermissions("vehHandleFlow:list")
    @ResponseBody
    public AjaxResult openCount() {
        return AjaxResult.success(200, "在办卷张数（回库现点）", vehHandleFlowService.countOpen());
    }

    /** 问话那一刻：空着按问话当下；写法不对当场点明，不 silently 当下 */
    private AjaxResult badMoment(String text) {
        if (text != null && !text.trim().isEmpty() && parseWallClock(text) == null) {
            return error("问话那一刻写法不对，认 " + FMT_TS);
        }
        return null;
    }

    private Date momentOrNow(String text) {
        Date d = parseWallClock(text);
        return d == null ? new Date() : d;
    }

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
