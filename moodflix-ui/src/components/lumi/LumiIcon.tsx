import { FC } from "react";

interface LumiIconProps {
  size?: number;
  className?: string;
}

const LumiIcon: FC<LumiIconProps> = ({ size = 48, className = "" }) => {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      {/* Head — rounded dome */}
      <path d="M6 11c0-3.3 2.7-6 6-6s6 2.7 6 6v1H6v-1z" />

      {/* Eyes */}
      <circle cx="9.5" cy="9" r="1" fill="currentColor" stroke="none" />
      <circle cx="14.5" cy="9" r="1" fill="currentColor" stroke="none" />

      {/* Tentacles */}
      <path d="M7 12c0 2.5-1.5 4-1.5 5.5a1.5 1.5 0 0 0 3 0" />
      <path d="M10 12v4.5a1.5 1.5 0 0 0 3 0V12" />
      <path d="M17 12c0 2.5 1.5 4 1.5 5.5a1.5 1.5 0 0 1-3 0" />
    </svg>
  );
};

export default LumiIcon;
