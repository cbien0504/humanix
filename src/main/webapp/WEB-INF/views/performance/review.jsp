<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="http://hrm.example.com/functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="r" value="${view.review()}"/>
<c:set var="mode" value="${view.canSelfAssess() ? 'self' : (view.canManagerAssess() ? 'manager' : 'readonly')}"/>
<t:layout title="Phiếu đánh giá ${r.fullName}" active="${mode == 'manager' ? 'performance' : 'my-reviews'}" script="performance-review">
    <div class="page-header" id="reviewPage" data-review-id="${r.reviewId}" data-mode="${mode}">
        <div>
            <h1 class="h4 mb-1">Phiếu đánh giá: <c:out value="${r.fullName}"/></h1>
            <div class="subtitle"><c:out value="${r.employeeCode}"/> · <c:out value="${r.positionName}"/> · <c:out value="${r.departmentName}"/>
                · Kỳ <c:out value="${r.cycleName}"/> (<c:out value="${h:date(r.periodStart)}"/> – <c:out value="${h:date(r.periodEnd)}"/>)
                · Người đánh giá: <c:out value="${empty r.reviewerName ? '—' : r.reviewerName}"/></div>
        </div>
        <span class="badge fs-6 ${r.status == 'COMPLETED' ? 'text-bg-success' : r.status == 'PENDING_MANAGER' ? 'text-bg-info' : 'text-bg-warning'}"><c:out value="${r.status.label}"/></span>
    </div>
    <c:if test="${mode == 'self'}"><div class="alert alert-info">Hãy tự chấm từ 1 (chưa đạt) đến 5 (xuất sắc) cho từng tiêu chí và gửi phiếu cho quản lý trực tiếp.</div></c:if>
    <c:if test="${mode == 'manager'}"><div class="alert alert-info">Nhân viên đã tự đánh giá. Hãy chấm điểm từng tiêu chí và gửi kết quả – điểm cuối được tính tự động.</div></c:if>
    <form id="scoreForm" novalidate>
        <div class="alert alert-danger d-none form-alert"></div>
        <div class="card shadow-sm mb-3">
            <div class="table-responsive">
                <table class="table align-middle mb-0">
                    <thead><tr><th>Tiêu chí</th><th class="text-center">Trọng số</th><th class="text-center">Tự đánh giá</th><th class="text-center">Quản lý</th></tr></thead>
                    <tbody>
                    <c:forEach items="${r.scores}" var="s">
                        <tr>
                            <td><div class="fw-semibold"><c:out value="${s.name}"/></div><div class="small text-body-secondary"><c:out value="${s.description}"/></div></td>
                            <td class="text-center"><c:out value="${h:number(s.weight)}"/>%</td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${mode == 'self'}">
                                        <select class="form-select score-select mx-auto" name="score_${s.criteriaId}" data-criteria="${s.criteriaId}" required>
                                            <option value="">–</option><option>1</option><option>2</option><option>3</option><option>4</option><option>5</option>
                                        </select>
                                    </c:when>
                                    <c:otherwise><span class="fs-5"><c:out value="${empty s.selfScore ? '–' : s.selfScore}"/></span></c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${mode == 'manager'}">
                                        <select class="form-select score-select mx-auto" name="score_${s.criteriaId}" data-criteria="${s.criteriaId}" required>
                                            <option value="">–</option><option>1</option><option>2</option><option>3</option><option>4</option><option>5</option>
                                        </select>
                                    </c:when>
                                    <c:otherwise><span class="fs-5"><c:out value="${empty s.managerScore ? '–' : s.managerScore}"/></span></c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="row g-3">
            <div class="col-lg-6">
                <div class="card shadow-sm h-100"><div class="card-body">
                    <label class="form-label fw-semibold" for="selfComment">Nhân viên tự nhận xét</label>
                    <c:choose>
                        <c:when test="${mode == 'self'}"><textarea class="form-control" id="selfComment" name="comment" rows="4" maxlength="2000"></textarea></c:when>
                        <c:otherwise><p class="mb-0"><c:out value="${empty r.selfComment ? '—' : r.selfComment}"/></p></c:otherwise>
                    </c:choose>
                </div></div>
            </div>
            <div class="col-lg-6">
                <div class="card shadow-sm h-100"><div class="card-body">
                    <label class="form-label fw-semibold" for="managerComment">Quản lý nhận xét</label>
                    <c:choose>
                        <c:when test="${mode == 'manager'}"><textarea class="form-control" id="managerComment" name="comment" rows="4" maxlength="2000"></textarea></c:when>
                        <c:otherwise><p class="mb-0"><c:out value="${empty r.managerComment ? '—' : r.managerComment}"/></p></c:otherwise>
                    </c:choose>
                </div></div>
            </div>
        </div>
        <c:if test="${r.status == 'COMPLETED'}">
            <div class="card shadow-sm mt-3"><div class="card-body d-flex flex-wrap gap-4 align-items-center">
                <div><div class="stat-label">Tự đánh giá</div><div class="stat-value"><c:out value="${h:number(r.selfScore)}"/></div></div>
                <div><div class="stat-label">Quản lý</div><div class="stat-value"><c:out value="${h:number(r.managerScore)}"/></div></div>
                <div><div class="stat-label">Điểm cuối</div><div class="stat-value text-primary"><c:out value="${h:number(r.finalScore)}"/></div></div>
                <div><div class="stat-label">Xếp loại</div><div class="stat-value"><span class="badge text-bg-primary rating-badge"><c:out value="${r.rating}"/></span></div></div>
            </div></div>
        </c:if>
        <div class="mt-3 d-flex gap-2">
            <a class="btn btn-light" href="${pageContext.request.contextPath}${mode == 'manager' ? '/performance/manage' : '/performance'}"><i class="bi bi-arrow-left me-1"></i>Quay lại</a>
            <c:if test="${mode != 'readonly'}"><button class="btn btn-primary" type="submit"><i class="bi bi-send me-1"></i>Gửi đánh giá</button></c:if>
        </div>
    </form>
</t:layout>
