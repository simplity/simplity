/**
 * http utility classes required to 1. One of the security requirements is that the client MUST
 * return back some fields unchanged. programmers tend to just define them as hidden fields.
 * Obviously this is vulnerable. We follow a simple principle. if you want the same thing back, just
 * don't send it. But then, where do we keep and how do we associate the fields back to the right
 * transaction? POST_BACK concept of .net is a good idea. We will provide a way for a service output
 * to mark a sheet as post back. this data is saved with a uid, and the uid is sent to client. When
 * the client returns, uid is used to push saved fields to input stream. TODO: how do we tag
 * services that require a valid uid? May be an api similar to user-based security!!
 *
 * @author simplity.org
 */
package org.simplity.kernel.http;
