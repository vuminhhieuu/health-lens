import { z } from "zod";

export {
  registerSchema,
  type RegisterInput,
  changePasswordSchema,
  type ChangePasswordInput,
  passwordSchema,
} from "./auth";
export {
  updateUserProfileSchema,
  updateHealthContextSchema,
  type UpdateUserProfileInput,
  type UpdateHealthContextInput,
} from "./user";
export * from "./profile";

export const userSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email(),
});

export const profileSchema = z.object({
  id: z.string().uuid(),
  userId: z.string().uuid(),
  displayName: z.string().min(1),
});

export const healthRecordSchema = z.object({
  id: z.string().uuid(),
  profileId: z.string().uuid(),
  examDate: z.string().date(),
});
