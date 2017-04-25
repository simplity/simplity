FreeMarker Template example: 
<div style="color:red;">Mail Formatting :</div>
=======================<br>
========Employee=======<br>
=======================<br>
<#list name as names>
	${names_index + 1}. ${names} <br>
</#list>
<br>
<#list id as ids>
	${ids_index + 1}. ${ids} <br>
</#list>
<br>

<br>
<h1>This is a test</h1> <img src="cid:Quality1.gif"/>
<h1>This is a test</h1> <img src="cid:Quality2.gif"/>
