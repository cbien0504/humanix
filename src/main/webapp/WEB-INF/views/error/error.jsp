<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%-- Handles both errors forwarded by ErrorHandler (errorStatus/errorMessage) and
     container error pages (jakarta.servlet.error.*). Never shows stack traces. --%>
<c:set var="status" value="${not empty errorStatus ? errorStatus : requestScope['jakarta.servlet.error.status_code']}"/>
<c:if test="${empty status}"><c:set var="status" value="500"/></c:if>
<c:choose>
    <c:when test="${not empty errorMessage}"><c:set var="msg" value="${errorMessage}"/></c:when>
    <c:when test="${status == 404}"><c:set var="msg" value="Không tìm thấy trang bạn yêu cầu."/></c:when>
    <c:when test="${status == 403}"><c:set var="msg" value="Bạn không có quyền truy cập chức năng này."/></c:when>
    <c:otherwise><c:set var="msg" value="Đã xảy ra lỗi hệ thống. Vui lòng thử lại hoặc liên hệ bộ phận IT kèm mã tra cứu."/></c:otherwise>
</c:choose>
<t:layout title="Lỗi ${status}">
    <div class="row justify-content-center mt-5">
        <div class="col-md-8 col-lg-6 text-center">
            <div class="display-4 fw-bold text-primary mb-2"><c:out value="${status}"/></div>
            <p class="lead"><c:out value="${msg}"/></p>
            <c:if test="${not empty requestScope.requestId}">
                <p class="text-body-secondary small">Mã tra cứu: <code><c:out value="${requestScope.requestId}"/></code></p>
            </c:if>
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/"><i class="bi bi-house me-1"></i>Về trang chủ</a>
        </div>
    </div>
</t:layout>
