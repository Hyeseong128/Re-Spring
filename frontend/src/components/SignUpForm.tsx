"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { signup, login } from "@/lib/api"
import { useRouter } from "next/navigation"

export function SignUpForm() {
  // 상태 관리를 위한 useState 훅 사용
  const [username, setUsername] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")

  const router = useRouter();

  // 폼 제출 핸들러
  const handleSubmit = async (e: React.FormEvent) => {
    try{
      const signUpResult = await signup(username, email, password);

      if(signUpResult){
        window.location.reload();
      }else{
        alert('회원가입 실패')
      }
    }catch(error){
      alert("회원가입 실패");
      console.error(error);
    }
    e.preventDefault()
  }

  return (
    <Card className="w-full max-w-md mx-auto">
      <CardHeader>
        <CardTitle>회원가입</CardTitle>
        <CardDescription>새로운 계정을 만들어보세요.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="username">사용자 이름</Label>
            <Input
              id="username"
              type="text"
              placeholder="홍길동"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">이메일</Label>
            <Input
              id="email"
              type="email"
              placeholder="example@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">비밀번호</Label>
            <Input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <Button type="submit" className="w-full">
            가입하기
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

