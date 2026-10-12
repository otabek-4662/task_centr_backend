package com.taskcenter.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RealWorld (Conduit) uslubidagi avtorizatsiyadan o'tgan joriy foydalanuvchini
 * Controller metodlariga to'g'ridan-to'g'ri in'ektsiya qilish uchun maxsus annotatsiya.
 *
 * Misol:
 * <pre>
 *   &#64;GetMapping("/me")
 *   public ApiResponse&lt;UserDto&gt; getProfile(&#64;CurrentUser User currentUser) {
 *       return ApiResponse.success("ok", userService.getProfile(currentUser));
 *   }
 * </pre>
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
