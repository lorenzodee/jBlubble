<%@ include file="/WEB-INF/common/_taglibs.jspf" %><!DOCTYPE html>
<html>
<head><title>jBlubble Sample</title></head>
<body>
<h1>Persons</h1>
<p>Add a person <a href="<c:url value='/persons/create' />">here</a>.</p>
<c:if test="${not empty persons}">
<table border="1" cellpadding="10">
	<tr>
		<th>Id</th><th>Name</th><th>Photo</th>
	</tr><c:forEach var="person" items="${persons}">
	<tr>
		<td><a href="<c:url value='/persons/${person.id}' />"><c:out value="${person.id}" /></a></td>
		<td><a href="<c:url value='/persons/${person.id}' />"><c:out value="${person.name}" /></a></td>
		<td><img height="100" src="<c:url value='/persons/${person.id}/photo' />" /></td>
	</tr>
	</c:forEach>
</table>
</c:if>
</body>
</html>
