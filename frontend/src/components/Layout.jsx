import { Outlet } from 'react-router-dom'
import Header from './Header.jsx'

export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col bg-bg">
      <Header />
      <main className="flex flex-1 flex-col px-10 py-6">
        <Outlet />
      </main>
    </div>
  )
}
