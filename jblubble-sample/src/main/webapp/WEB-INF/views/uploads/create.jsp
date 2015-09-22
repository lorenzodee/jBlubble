<%@ include file="/WEB-INF/common/_taglibs.jspf" %>
<!DOCTYPE html>
<html>
<head><title>jBlubble Sample</title></head>
<body>
<h1>New Upload</h1>
<form action="<c:url value='/uploads' />" method="post" enctype="multipart/form-data">
	<div>
		<label>File</label>
		<input type="file" name="file" />
	</div>
	<div>
		<button>Upload</button>
		<a href="<c:url value='/uploads' />">Cancel</a>
	</div>
</form>
</body>
</html>
